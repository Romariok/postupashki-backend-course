package mutex

import (
	"primitives/internal/futex"
	"sync/atomic"
)

const (
	free = iota
	held
	contended
)

type Mutex struct {
	state uint32
}

func (m *Mutex) Lock() {
	if m.TryLock() {
		return
	}

	for range 10 {
		if m.TryLock() {
			return
		}
	}

	for atomic.SwapUint32(&m.state, contended) != free {
		futex.Wait(&m.state, contended)
	}
}

func (m *Mutex) TryLock() bool {
	return atomic.CompareAndSwapUint32(&m.state, free, held)
}

func (m *Mutex) Unlock() {
	switch atomic.SwapUint32(&m.state, free) {
	case held:
	case contended:
		futex.Wake(&m.state)
	default:
		panic("unlock of unlocked mutex")
	}
}

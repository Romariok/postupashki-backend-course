package mutex

import (
	"sync/atomic"
	"primitives/internal/futex"
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

	for {
		if atomic.LoadUint32(&m.state) == contended || !atomic.CompareAndSwapUint32(&m.state, held, contended) {
			futex.Wait(&m.state, contended)
		}
		if atomic.CompareAndSwapUint32(&m.state, free, contended) {
			break
		}
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

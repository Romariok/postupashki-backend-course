package rwmutex

import (
	"sync/atomic"
	"primitives/internal/futex"
)

const writer = 1 << 31

type RWMutex struct {
	state uint32
}

func (rw *RWMutex) RLock() {
	for {
		s := atomic.LoadUint32(&rw.state)
		if s&writer != 0 {
			futex.Wait(&rw.state, s)
			continue
		}
		if atomic.CompareAndSwapUint32(&rw.state, s, s+1) {
			return
		}
	}
}

func (rw *RWMutex) RUnlock() {
	if atomic.LoadUint32(&rw.state) == 0 {
		panic("RUnlock of unlocked RWMutex")
	}
	for {
		s := atomic.LoadUint32(&rw.state)
		if s&writer != 0 {
			futex.Wait(&rw.state, s)
			continue
		}
		if atomic.CompareAndSwapUint32(&rw.state, s, s-1) {
			if s == 1 {
				futex.WakeAll(&rw.state)
			}
			return
		}
	}
}

func (rw *RWMutex) Lock() {
	for {
		s := atomic.LoadUint32(&rw.state)
		if s != 0 {
			futex.Wait(&rw.state, s)
			continue
		}
		if atomic.CompareAndSwapUint32(&rw.state, s, writer) {
			return
		}
	}
}

func (rw *RWMutex) Unlock() {
	if atomic.CompareAndSwapUint32(&rw.state, writer, 0) {
        futex.WakeAll(&rw.state)
        return
  }
  panic("Unlock of unlocked RWMutex")
}

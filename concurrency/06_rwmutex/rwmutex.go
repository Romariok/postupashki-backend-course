package rwmutex

import (
	"primitives/internal/futex"
	"runtime"
	"sync/atomic"
)

const writer = 1 << 31
const readers = writer - 1

type RWMutex struct {
	state   uint32
	waiters uint32
	writers uint32
}

func (rw *RWMutex) RLock() {
	for {
		if atomic.LoadUint32(&rw.writers) != 0 {
			if state := atomic.LoadUint32(&rw.state); state == 0 {
				runtime.Gosched()
			} else {
				rw.wait(state)
			}
			continue
		}
		s := atomic.LoadUint32(&rw.state)
		if s&writer != 0 {
			rw.wait(s)
			continue
		}
		if s&readers == readers {
			panic("too many readers")
		}
		if atomic.CompareAndSwapUint32(&rw.state, s, s+1) {
			if atomic.LoadUint32(&rw.writers) == 0 {
				return
			}
			rw.RUnlock()
		}
	}
}

func (rw *RWMutex) RUnlock() {
	for {
		s := atomic.LoadUint32(&rw.state)
		if s&writer != 0 || s&readers == 0 {
			panic("RUnlock of unlocked RWMutex")
		}
		if atomic.CompareAndSwapUint32(&rw.state, s, s-1) {
			if s&readers == 1 && atomic.LoadUint32(&rw.waiters) != 0 {
				futex.WakeAll(&rw.state)
			}
			return
		}
	}
}

func (rw *RWMutex) Lock() {
	atomic.AddUint32(&rw.writers, 1)
	for {
		s := atomic.LoadUint32(&rw.state)
		if s == 0 {
			if atomic.CompareAndSwapUint32(&rw.state, 0, writer) {
				atomic.AddUint32(&rw.writers, ^uint32(0))
				return
			}
			continue
		}
		rw.wait(s)
	}
}

func (rw *RWMutex) Unlock() {
	if atomic.CompareAndSwapUint32(&rw.state, writer, 0) {
		if atomic.LoadUint32(&rw.waiters) != 0 {
			futex.WakeAll(&rw.state)
		}
		return
	}
	panic("Unlock of unlocked RWMutex")
}

func (rw *RWMutex) wait(state uint32) {
	atomic.AddUint32(&rw.waiters, 1)
	futex.Wait(&rw.state, state)
	atomic.AddUint32(&rw.waiters, ^uint32(0))
}

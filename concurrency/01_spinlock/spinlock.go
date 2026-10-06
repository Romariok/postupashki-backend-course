package spinlock

import (
	"runtime"
	"sync/atomic"
)

const yieldEvery = 64

type Spinlock struct {
	locked atomic.Bool
}

func (s *Spinlock) Lock() {
	for failures := 1; !s.locked.CompareAndSwap(false, true); failures++ {
		if failures%yieldEvery == 0 {
			runtime.Gosched()
		}
	}
}

func (s *Spinlock) TryLock() bool {
	return s.locked.CompareAndSwap(false, true)
}

func (s *Spinlock) Unlock() {
	if !s.locked.CompareAndSwap(true, false) {
		panic("unlock of unlocked lock")
	}
}

type TTAS struct {
	locked atomic.Bool
}

func (s *TTAS) Lock() {
	spins := 0
	for {
		for s.locked.Load() {
			spins++
			if spins%yieldEvery == 0 {
				runtime.Gosched()
			}
		}
		if s.locked.CompareAndSwap(false, true) {
			return
		}
		spins++
		if spins%yieldEvery == 0 {
			runtime.Gosched()
		}
	}
}

func (s *TTAS) TryLock() bool {
	return s.locked.CompareAndSwap(false, true)
}

func (s *TTAS) Unlock() {
	if !s.locked.CompareAndSwap(true, false) {
		panic("unlock of unlocked lock")
	}
}

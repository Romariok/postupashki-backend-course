package semaphore

import (
	"primitives/internal/futex"
	"sync/atomic"
)

type Semaphore struct {
	permits uint32
	waiters uint32
}

func New(n int) *Semaphore {
	if n < 0 || uint64(n) > uint64(^uint32(0)) {
		panic("invalid semaphore size")
	}
	return &Semaphore{permits: uint32(n)}
}

func (s *Semaphore) Acquire() {
	for !s.TryAcquire() {
		atomic.AddUint32(&s.waiters, 1)
		futex.Wait(&s.permits, 0)
		atomic.AddUint32(&s.waiters, ^uint32(0))
	}
}

func (s *Semaphore) TryAcquire() bool {
	for permits := atomic.LoadUint32(&s.permits); permits > 0; permits = atomic.LoadUint32(&s.permits) {
		if atomic.CompareAndSwapUint32(&s.permits, permits, permits-1) {
			return true
		}
	}
	return false
}

func (s *Semaphore) Release() {
	for {
		permits := atomic.LoadUint32(&s.permits)
		if permits == ^uint32(0) {
			panic("semaphore overflow")
		}
		if atomic.CompareAndSwapUint32(&s.permits, permits, permits+1) {
			if atomic.LoadUint32(&s.waiters) != 0 {
				futex.Wake(&s.permits)
			}
			return
		}
	}
}

func (s *Semaphore) Available() int {
	return int(atomic.LoadUint32(&s.permits))
}

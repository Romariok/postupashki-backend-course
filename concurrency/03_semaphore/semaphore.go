package semaphore

import (
	"sync/atomic"
	"primitives/internal/futex"
)

type Semaphore struct {
	permits uint32
}

func New(n int) *Semaphore {
	return &Semaphore{permits: uint32(n)}
}

func (s *Semaphore) Acquire() {
	for !s.TryAcquire() {
		futex.Wait(&s.permits, 0)
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
	atomic.AddUint32(&s.permits, 1)
	futex.Wake(&s.permits)
}

func (s *Semaphore) Available() int {
	return int(atomic.LoadUint32(&s.permits))
}

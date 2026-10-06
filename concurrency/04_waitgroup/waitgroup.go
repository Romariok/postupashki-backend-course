package waitgroup

import (
	"primitives/internal/futex"
	"sync/atomic"
)

type WaitGroup struct {
	count   uint32
	waiters uint32
}

func (wg *WaitGroup) Add(delta int) {
	count := atomic.AddUint32(&wg.count, uint32(delta))
	if int32(count) < 0 {
		panic("negative WaitGroup count")
	}
	if count == 0 && atomic.LoadUint32(&wg.waiters) != 0 {
		futex.WakeAll(&wg.count)
	}
}

func (wg *WaitGroup) Done() {
	wg.Add(-1)
}

func (wg *WaitGroup) Wait() {
	for {
		count := atomic.LoadUint32(&wg.count)
		if count == 0 {
			return
		}
		atomic.AddUint32(&wg.waiters, 1)
		futex.Wait(&wg.count, count)
		atomic.AddUint32(&wg.waiters, ^uint32(0))
	}
}

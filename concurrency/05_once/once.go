package once

import (
	"sync/atomic"
	"primitives/internal/futex"
)

type Once struct {
	state uint32
}

func (o *Once) Do(f func()) {
	defer func() {
		if r := recover(); r != nil {
			atomic.StoreUint32(&o.state, 2)
			futex.WakeAll(&o.state)
		}
	}()
	

	if atomic.CompareAndSwapUint32(&o.state, 0, 1) {
		f()
		atomic.StoreUint32(&o.state, 2)
		futex.WakeAll(&o.state)
		return
	}
	
	futex.Wait(&o.state, 1)
}

func (o *Once) Done() bool {
	return atomic.LoadUint32(&o.state) == 2
}

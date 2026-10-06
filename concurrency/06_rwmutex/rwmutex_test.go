package rwmutex

import (
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func finished(f func()) <-chan struct{} {
	ch := make(chan struct{})
	go func() {
		f()
		close(ch)
	}()
	return ch
}

func TestWriterExcludesEveryone(t *testing.T) {
	var rw RWMutex
	rw.Lock()

	reader := finished(func() {
		rw.RLock()
		rw.RUnlock()
	})
	writer := finished(func() {
		rw.Lock()
		rw.Unlock()
	})

	select {
	case <-reader:
		t.Fatal("читатель прошёл, пока держит писатель")
	case <-writer:
		t.Fatal("второй писатель прошёл, пока держит первый")
	case <-time.After(50 * time.Millisecond):
	}

	rw.Unlock()

	select {
	case <-reader:
	case <-time.After(2 * time.Second):
		t.Fatal("читатель не проснулся")
	}
	select {
	case <-writer:
	case <-time.After(2 * time.Second):
		t.Fatal("писатель не проснулся")
	}
}

func TestReadersGoTogether(t *testing.T) {
	var rw RWMutex
	const readers = 8

	var inside, peak int32
	var wg sync.WaitGroup

	for i := 0; i < readers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			rw.RLock()

			now := atomic.AddInt32(&inside, 1)
			for {
				old := atomic.LoadInt32(&peak)
				if now <= old || atomic.CompareAndSwapInt32(&peak, old, now) {
					break
				}
			}
			time.Sleep(30 * time.Millisecond)
			atomic.AddInt32(&inside, -1)

			rw.RUnlock()
		}()
	}

	select {
	case <-finished(wg.Wait):
	case <-time.After(5 * time.Second):
		t.Fatal("читатели зависли")
	}

	if peak < 2 {
		t.Fatalf("читатели шли по одному (пик %d) — смысл RWMutex теряется", peak)
	}
}

func TestWriterWaitsForReaders(t *testing.T) {
	var rw RWMutex
	rw.RLock()
	rw.RLock()

	writer := finished(func() {
		rw.Lock()
		rw.Unlock()
	})

	select {
	case <-writer:
		t.Fatal("писатель прошёл при живых читателях")
	case <-time.After(50 * time.Millisecond):
	}

	rw.RUnlock()

	select {
	case <-writer:
		t.Fatal("писатель прошёл, пока остался один читатель")
	case <-time.After(50 * time.Millisecond):
	}

	rw.RUnlock()

	select {
	case <-writer:
	case <-time.After(2 * time.Second):
		t.Fatal("писатель не проснулся после последнего RUnlock")
	}
}

func TestNoTornState(t *testing.T) {
	var rw RWMutex
	shared := 0
	bad := int32(0)

	var wg sync.WaitGroup

	for i := 0; i < 4; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < 5000; j++ {
				rw.Lock()
				shared++
				shared++
				rw.Unlock()
			}
		}()
	}

	for i := 0; i < 8; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < 5000; j++ {
				rw.RLock()
				if shared%2 != 0 {
					atomic.StoreInt32(&bad, 1)
				}
				rw.RUnlock()
			}
		}()
	}

	select {
	case <-finished(wg.Wait):
	case <-time.After(30 * time.Second):
		t.Fatal("нагрузочный тест завис")
	}

	if bad != 0 {
		t.Fatal("читатель увидел состояние в середине записи")
	}
	if shared != 4*5000*2 {
		t.Fatalf("записи потерялись: %d", shared)
	}
}

func TestUnlockWithoutLockPanics(t *testing.T) {
	t.Run("Unlock", func(t *testing.T) {
		defer func() {
			if recover() == nil {
				t.Fatal("Unlock без Lock должен паниковать")
			}
		}()
		var rw RWMutex
		rw.Unlock()
	})

	t.Run("RUnlock", func(t *testing.T) {
		defer func() {
			if recover() == nil {
				t.Fatal("RUnlock без RLock должен паниковать")
			}
		}()
		var rw RWMutex
		rw.RUnlock()
	})

	t.Run("RUnlockWhileWriterLocked", func(t *testing.T) {
		defer func() {
			if recover() == nil {
				t.Fatal("RUnlock при захваченном writer lock должен паниковать")
			}
		}()
		var rw RWMutex
		rw.Lock()
		rw.RUnlock()
	})
}

func TestConcurrentDoubleRUnlockPanics(t *testing.T) {
	var rw RWMutex
	rw.RLock()
	panicked := make(chan bool, 2)
	for range 2 {
		go func() {
			didPanic := false
			defer func() {
				if recover() != nil {
					didPanic = true
				}
				panicked <- didPanic
			}()
			rw.RUnlock()
		}()
	}
	if first, second := <-panicked, <-panicked; first == second {
		t.Fatalf("ровно один из двух RUnlock должен паниковать, получено %v и %v", first, second)
	}
}

func TestWaitingWriterBlocksNewReaders(t *testing.T) {
	var rw RWMutex
	rw.RLock()

	writerAcquired := make(chan struct{})
	releaseWriter := make(chan struct{})
	go func() {
		rw.Lock()
		close(writerAcquired)
		<-releaseWriter
		rw.Unlock()
	}()

	deadline := time.Now().Add(time.Second)
	for atomic.LoadUint32(&rw.writers) == 0 && time.Now().Before(deadline) {
		time.Sleep(time.Millisecond)
	}
	if atomic.LoadUint32(&rw.writers) == 0 {
		t.Fatal("писатель не объявил об ожидании")
	}

	readerAcquired := finished(func() {
		rw.RLock()
		rw.RUnlock()
	})
	rw.RUnlock()

	select {
	case <-readerAcquired:
		t.Fatal("новый читатель обогнал ожидающего писателя")
	case <-writerAcquired:
	}
	close(releaseWriter)
	select {
	case <-readerAcquired:
	case <-time.After(time.Second):
		t.Fatal("читатель не проснулся после писателя")
	}
}

func TestReaderDoesNotSleepOnZeroState(t *testing.T) {
	var rw RWMutex
	atomic.StoreUint32(&rw.writers, 1)
	reader := finished(func() {
		rw.RLock()
		rw.RUnlock()
	})

	time.Sleep(20 * time.Millisecond)
	atomic.StoreUint32(&rw.writers, 0)
	select {
	case <-reader:
	case <-time.After(time.Second):
		t.Fatal("читатель уснул на нулевом state и пропустил уход писателя")
	}
}

func BenchmarkReadHeavy(b *testing.B) {
	var rw RWMutex
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			rw.RLock()
			rw.RUnlock()
		}
	})
}

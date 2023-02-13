import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AsyncCache<K, V> {
    private static final ExecutorService asyncService = Executors.newCachedThreadPool();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private final Map<K, V> cache = new ConcurrentHashMap<>();

    private final Map<K, V> storage;

    public AsyncCache(Map<K, V> storage) {
        this.storage = storage;
    }

    public V put(K key, V value) {
        writeLock.lock();
        try {
            V result = cache.put(key, value);
            asyncService.submit(() -> save(key, value));
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    public V get(K key) {
        readLock.lock();
        try {
            V result;
            if (cache.containsKey(key)) {
                result = cache.get(key);
            } else {
                result = load(key);
            }
            return result;
        } finally {
            readLock.unlock();
        }
    }

    private void save(K key, V value) {
        storage.put(key, value);
    }

    private V load(K key) {
        return storage.get(key);
    }
}

package com.sok.core.Events;


import com.sok.core.Threading.CachedThreadPool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;

public class Event<T extends EventData>
{
    protected final ConcurrentMap<Integer, WeakReference<EventCallback<T>>> _weakListeners = new ConcurrentHashMap<>();
    protected final List<EventCallback<T>> _strongListeners = Collections.synchronizedList(new ArrayList<EventCallback<T>>());
    protected final ThreadPoolExecutor     _threadExecutor;

    public    final Event<CrossThreadExceptionEventData> ListenersThrowExceptionEvent;


    public Event () { this(false); }
    protected Event(boolean internal)
    {
        if (internal) {
            ListenersThrowExceptionEvent = null;
            _threadExecutor = null;
        }
        else {
            ListenersThrowExceptionEvent = new Event<>(true);
            _threadExecutor = new CachedThreadPool();
        }
    }


    public int ListenerCount()
    {
        return _strongListeners.size() + _weakListeners.size();
    }



    public synchronized void RegisterAsWeakReference(EventCallback<T> listener)
    {
        if (listener==null) return;
        Unregister(listener);
        _weakListeners.put(listener.hashCode(), new WeakReference<>(listener));
    }

    public synchronized void Register(EventCallback<T> listener)
    {
        if (listener==null) return;
        Unregister(listener);
        _strongListeners.add(listener);
    }

    public void Unregister(EventCallback<T> listener)
    {
        if (listener==null) return;
        // if listener or its hashKey does not exist, nothing removed.
        // So, no gard check required.
        _weakListeners.remove(listener.hashCode());
        _strongListeners.remove(listener);
    }

    public void Notify(final Object sender, final T data)
    {
        NotifyInternal(sender, data, false);
    }


    public void NotifyAsync(final Object sender, final T data)
    {
        NotifyInternal(sender, data, true);
    }



    private synchronized void NotifyInternal(final Object sender, final T data, final boolean isAsync)
    {
        if (ListenerCount()==0) return;
        try {

            boolean hasDeadListeners = false;
            for (WeakReference<EventCallback<T>> ref : _weakListeners.values()) {
                EventCallback<T> listener = ref.get();
                if (listener == null) hasDeadListeners = true;
                else {
                    if (isAsync) NotifyOnThreadPool(listener, sender, data);
                    else listener.OnEvent(sender, data);
                }
            }

            for (EventCallback<T> listener : _strongListeners)
            {
                if (isAsync) NotifyOnThreadPool(listener, sender, data);
                else listener.OnEvent(sender, data);
            }


            if (hasDeadListeners) RemoveDeadListeners();
        }
        catch (Exception ex) { OnError(ex); }
    }



    private void NotifyOnThreadPool(final EventCallback<T> client, final Object sender, final T data)
    {
        _threadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                client.OnEvent(sender, data);
            }
        });
    }



    private synchronized void RemoveDeadListeners()
    {
        Set<Integer> keys = _weakListeners.keySet();
        ArrayList<Integer> deadObj = new ArrayList<>();
        for (Integer key : keys) {
            WeakReference<EventCallback<T>> objRef = _weakListeners.get(key);
            if (objRef.get() == null) deadObj.add(key);
        }
        for (Integer key : deadObj) _weakListeners.remove(key);
    }


    private void OnError(final Exception ex)
    {
        ex.printStackTrace();
        ListenersThrowExceptionEvent.Notify(this, new CrossThreadExceptionEventData(ex));
    }



    @Override
    protected synchronized void finalize() throws Throwable {
        super.finalize();
        if(_threadExecutor!=null && !_threadExecutor.isTerminated()) _threadExecutor.shutdown();
    }
}


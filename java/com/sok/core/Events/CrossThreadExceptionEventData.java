package com.sok.core.Events;

public class CrossThreadExceptionEventData extends EventData
{
    private Exception _exception;
    private long    _threadId;

    public CrossThreadExceptionEventData(Exception exception)
    {
        _exception = exception;
        _threadId = Thread.currentThread().getId();

    }

    public Exception Exception() {
        return _exception;
    }
    public long ThreadId() { return _threadId; }
}

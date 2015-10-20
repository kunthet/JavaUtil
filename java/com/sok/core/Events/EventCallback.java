package com.sok.core.Events;

public abstract class EventCallback<T extends EventData>
{
    public abstract  void OnEvent(Object sender,T data);

    public EventCallback() {
        super();
    }

}

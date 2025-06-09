package org.las2mile.scrcpy.wrappers;

import android.os.IInterface;
import android.view.InputEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class InputManager {
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    private final IInterface manager;
    private final Method injectInputEventMethod;

    public InputManager(IInterface manager) {
        this.manager = manager;
        try {
            injectInputEventMethod = manager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public boolean injectInputEvent(InputEvent inputEvent, int mode) {
        try {
            return (Boolean) injectInputEventMethod.invoke(manager, inputEvent, mode);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}

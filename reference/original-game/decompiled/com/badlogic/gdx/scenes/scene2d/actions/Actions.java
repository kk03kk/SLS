/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.actions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.AddAction;
import com.badlogic.gdx.scenes.scene2d.actions.AddListenerAction;
import com.badlogic.gdx.scenes.scene2d.actions.AfterAction;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.ColorAction;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.LayoutAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.RemoveAction;
import com.badlogic.gdx.scenes.scene2d.actions.RemoveActorAction;
import com.badlogic.gdx.scenes.scene2d.actions.RemoveListenerAction;
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction;
import com.badlogic.gdx.scenes.scene2d.actions.RotateByAction;
import com.badlogic.gdx.scenes.scene2d.actions.RotateToAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleByAction;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.actions.SizeByAction;
import com.badlogic.gdx.scenes.scene2d.actions.SizeToAction;
import com.badlogic.gdx.scenes.scene2d.actions.TimeScaleAction;
import com.badlogic.gdx.scenes.scene2d.actions.TouchableAction;
import com.badlogic.gdx.scenes.scene2d.actions.VisibleAction;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public class Actions {
    public static <T extends Action> T action(Class<T> type) {
        Pool<T> pool = Pools.get(type);
        Action action = (Action)pool.obtain();
        action.setPool(pool);
        return (T)action;
    }

    public static AddAction addAction(Action action) {
        AddAction addAction = Actions.action(AddAction.class);
        addAction.setAction(action);
        return addAction;
    }

    public static AddAction addAction(Action action, Actor targetActor) {
        AddAction addAction = Actions.action(AddAction.class);
        addAction.setTarget(targetActor);
        addAction.setAction(action);
        return addAction;
    }

    public static RemoveAction removeAction(Action action) {
        RemoveAction removeAction = Actions.action(RemoveAction.class);
        removeAction.setAction(action);
        return removeAction;
    }

    public static RemoveAction removeAction(Action action, Actor targetActor) {
        RemoveAction removeAction = Actions.action(RemoveAction.class);
        removeAction.setTarget(targetActor);
        removeAction.setAction(action);
        return removeAction;
    }

    public static MoveToAction moveTo(float x, float y) {
        return Actions.moveTo(x, y, 0.0f, null);
    }

    public static MoveToAction moveTo(float x, float y, float duration) {
        return Actions.moveTo(x, y, duration, null);
    }

    public static MoveToAction moveTo(float x, float y, float duration, Interpolation interpolation) {
        MoveToAction action = Actions.action(MoveToAction.class);
        action.setPosition(x, y);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static MoveToAction moveToAligned(float x, float y, int alignment) {
        return Actions.moveToAligned(x, y, alignment, 0.0f, null);
    }

    public static MoveToAction moveToAligned(float x, float y, int alignment, float duration) {
        return Actions.moveToAligned(x, y, alignment, duration, null);
    }

    public static MoveToAction moveToAligned(float x, float y, int alignment, float duration, Interpolation interpolation) {
        MoveToAction action = Actions.action(MoveToAction.class);
        action.setPosition(x, y, alignment);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static MoveByAction moveBy(float amountX, float amountY) {
        return Actions.moveBy(amountX, amountY, 0.0f, null);
    }

    public static MoveByAction moveBy(float amountX, float amountY, float duration) {
        return Actions.moveBy(amountX, amountY, duration, null);
    }

    public static MoveByAction moveBy(float amountX, float amountY, float duration, Interpolation interpolation) {
        MoveByAction action = Actions.action(MoveByAction.class);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static SizeToAction sizeTo(float x, float y) {
        return Actions.sizeTo(x, y, 0.0f, null);
    }

    public static SizeToAction sizeTo(float x, float y, float duration) {
        return Actions.sizeTo(x, y, duration, null);
    }

    public static SizeToAction sizeTo(float x, float y, float duration, Interpolation interpolation) {
        SizeToAction action = Actions.action(SizeToAction.class);
        action.setSize(x, y);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static SizeByAction sizeBy(float amountX, float amountY) {
        return Actions.sizeBy(amountX, amountY, 0.0f, null);
    }

    public static SizeByAction sizeBy(float amountX, float amountY, float duration) {
        return Actions.sizeBy(amountX, amountY, duration, null);
    }

    public static SizeByAction sizeBy(float amountX, float amountY, float duration, Interpolation interpolation) {
        SizeByAction action = Actions.action(SizeByAction.class);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static ScaleToAction scaleTo(float x, float y) {
        return Actions.scaleTo(x, y, 0.0f, null);
    }

    public static ScaleToAction scaleTo(float x, float y, float duration) {
        return Actions.scaleTo(x, y, duration, null);
    }

    public static ScaleToAction scaleTo(float x, float y, float duration, Interpolation interpolation) {
        ScaleToAction action = Actions.action(ScaleToAction.class);
        action.setScale(x, y);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static ScaleByAction scaleBy(float amountX, float amountY) {
        return Actions.scaleBy(amountX, amountY, 0.0f, null);
    }

    public static ScaleByAction scaleBy(float amountX, float amountY, float duration) {
        return Actions.scaleBy(amountX, amountY, duration, null);
    }

    public static ScaleByAction scaleBy(float amountX, float amountY, float duration, Interpolation interpolation) {
        ScaleByAction action = Actions.action(ScaleByAction.class);
        action.setAmount(amountX, amountY);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static RotateToAction rotateTo(float rotation) {
        return Actions.rotateTo(rotation, 0.0f, null);
    }

    public static RotateToAction rotateTo(float rotation, float duration) {
        return Actions.rotateTo(rotation, duration, null);
    }

    public static RotateToAction rotateTo(float rotation, float duration, Interpolation interpolation) {
        RotateToAction action = Actions.action(RotateToAction.class);
        action.setRotation(rotation);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static RotateByAction rotateBy(float rotationAmount) {
        return Actions.rotateBy(rotationAmount, 0.0f, null);
    }

    public static RotateByAction rotateBy(float rotationAmount, float duration) {
        return Actions.rotateBy(rotationAmount, duration, null);
    }

    public static RotateByAction rotateBy(float rotationAmount, float duration, Interpolation interpolation) {
        RotateByAction action = Actions.action(RotateByAction.class);
        action.setAmount(rotationAmount);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static ColorAction color(Color color) {
        return Actions.color(color, 0.0f, null);
    }

    public static ColorAction color(Color color, float duration) {
        return Actions.color(color, duration, null);
    }

    public static ColorAction color(Color color, float duration, Interpolation interpolation) {
        ColorAction action = Actions.action(ColorAction.class);
        action.setEndColor(color);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static AlphaAction alpha(float a) {
        return Actions.alpha(a, 0.0f, null);
    }

    public static AlphaAction alpha(float a, float duration) {
        return Actions.alpha(a, duration, null);
    }

    public static AlphaAction alpha(float a, float duration, Interpolation interpolation) {
        AlphaAction action = Actions.action(AlphaAction.class);
        action.setAlpha(a);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static AlphaAction fadeOut(float duration) {
        return Actions.alpha(0.0f, duration, null);
    }

    public static AlphaAction fadeOut(float duration, Interpolation interpolation) {
        AlphaAction action = Actions.action(AlphaAction.class);
        action.setAlpha(0.0f);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static AlphaAction fadeIn(float duration) {
        return Actions.alpha(1.0f, duration, null);
    }

    public static AlphaAction fadeIn(float duration, Interpolation interpolation) {
        AlphaAction action = Actions.action(AlphaAction.class);
        action.setAlpha(1.0f);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static VisibleAction show() {
        return Actions.visible(true);
    }

    public static VisibleAction hide() {
        return Actions.visible(false);
    }

    public static VisibleAction visible(boolean visible) {
        VisibleAction action = Actions.action(VisibleAction.class);
        action.setVisible(visible);
        return action;
    }

    public static TouchableAction touchable(Touchable touchable) {
        TouchableAction action = Actions.action(TouchableAction.class);
        action.setTouchable(touchable);
        return action;
    }

    public static RemoveActorAction removeActor() {
        return Actions.action(RemoveActorAction.class);
    }

    public static RemoveActorAction removeActor(Actor removeActor) {
        RemoveActorAction action = Actions.action(RemoveActorAction.class);
        action.setTarget(removeActor);
        return action;
    }

    public static DelayAction delay(float duration) {
        DelayAction action = Actions.action(DelayAction.class);
        action.setDuration(duration);
        return action;
    }

    public static DelayAction delay(float duration, Action delayedAction) {
        DelayAction action = Actions.action(DelayAction.class);
        action.setDuration(duration);
        action.setAction(delayedAction);
        return action;
    }

    public static TimeScaleAction timeScale(float scale, Action scaledAction) {
        TimeScaleAction action = Actions.action(TimeScaleAction.class);
        action.setScale(scale);
        action.setAction(scaledAction);
        return action;
    }

    public static SequenceAction sequence(Action action1) {
        SequenceAction action = Actions.action(SequenceAction.class);
        action.addAction(action1);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2) {
        SequenceAction action = Actions.action(SequenceAction.class);
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2, Action action3) {
        SequenceAction action = Actions.action(SequenceAction.class);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2, Action action3, Action action4) {
        SequenceAction action = Actions.action(SequenceAction.class);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        return action;
    }

    public static SequenceAction sequence(Action action1, Action action2, Action action3, Action action4, Action action5) {
        SequenceAction action = Actions.action(SequenceAction.class);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        action.addAction(action5);
        return action;
    }

    public static SequenceAction sequence(Action ... actions) {
        SequenceAction action = Actions.action(SequenceAction.class);
        int n = actions.length;
        for (int i = 0; i < n; ++i) {
            action.addAction(actions[i]);
        }
        return action;
    }

    public static SequenceAction sequence() {
        return Actions.action(SequenceAction.class);
    }

    public static ParallelAction parallel(Action action1) {
        ParallelAction action = Actions.action(ParallelAction.class);
        action.addAction(action1);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2) {
        ParallelAction action = Actions.action(ParallelAction.class);
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2, Action action3) {
        ParallelAction action = Actions.action(ParallelAction.class);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2, Action action3, Action action4) {
        ParallelAction action = Actions.action(ParallelAction.class);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        return action;
    }

    public static ParallelAction parallel(Action action1, Action action2, Action action3, Action action4, Action action5) {
        ParallelAction action = Actions.action(ParallelAction.class);
        action.addAction(action1);
        action.addAction(action2);
        action.addAction(action3);
        action.addAction(action4);
        action.addAction(action5);
        return action;
    }

    public static ParallelAction parallel(Action ... actions) {
        ParallelAction action = Actions.action(ParallelAction.class);
        int n = actions.length;
        for (int i = 0; i < n; ++i) {
            action.addAction(actions[i]);
        }
        return action;
    }

    public static ParallelAction parallel() {
        return Actions.action(ParallelAction.class);
    }

    public static RepeatAction repeat(int count, Action repeatedAction) {
        RepeatAction action = Actions.action(RepeatAction.class);
        action.setCount(count);
        action.setAction(repeatedAction);
        return action;
    }

    public static RepeatAction forever(Action repeatedAction) {
        RepeatAction action = Actions.action(RepeatAction.class);
        action.setCount(-1);
        action.setAction(repeatedAction);
        return action;
    }

    public static RunnableAction run(Runnable runnable) {
        RunnableAction action = Actions.action(RunnableAction.class);
        action.setRunnable(runnable);
        return action;
    }

    public static LayoutAction layout(boolean enabled) {
        LayoutAction action = Actions.action(LayoutAction.class);
        action.setLayoutEnabled(enabled);
        return action;
    }

    public static AfterAction after(Action action) {
        AfterAction afterAction = Actions.action(AfterAction.class);
        afterAction.setAction(action);
        return afterAction;
    }

    public static AddListenerAction addListener(EventListener listener, boolean capture) {
        AddListenerAction addAction = Actions.action(AddListenerAction.class);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }

    public static AddListenerAction addListener(EventListener listener, boolean capture, Actor targetActor) {
        AddListenerAction addAction = Actions.action(AddListenerAction.class);
        addAction.setTarget(targetActor);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }

    public static RemoveListenerAction removeListener(EventListener listener, boolean capture) {
        RemoveListenerAction addAction = Actions.action(RemoveListenerAction.class);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }

    public static RemoveListenerAction removeListener(EventListener listener, boolean capture, Actor targetActor) {
        RemoveListenerAction addAction = Actions.action(RemoveListenerAction.class);
        addAction.setTarget(targetActor);
        addAction.setListener(listener);
        addAction.setCapture(capture);
        return addAction;
    }
}


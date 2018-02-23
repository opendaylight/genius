/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.fcapsappjmx;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationBroadcasterSupport;

public class ControlPathFailureAlarm extends NotificationBroadcasterSupport implements ControlPathFailureAlarmMBean {

    private volatile List<String> raiseAlarmObject = new ArrayList<>();
    private volatile List<String> clearAlarmObject = new ArrayList<>();
    private final AtomicLong sequenceNumber = new AtomicLong(1);

    @Override
    public void setRaiseAlarmObject(List<String> raiseAlarmObject) {
        this.raiseAlarmObject = raiseAlarmObject;

        sendRaiseAlarmNotification(this.raiseAlarmObject);
    }

    private void sendRaiseAlarmNotification(List<String> alarmObject) {
        sendNotification(new AttributeChangeNotification(this, sequenceNumber.getAndIncrement(),
                System.currentTimeMillis(), "raise alarm object notified ", "raiseAlarmObject", "ArrayList", "",
                alarmObject));
    }

    @Override
    public List<String> getRaiseAlarmObject() {
        return Collections.unmodifiableList(raiseAlarmObject);
    }

    @Override
    public void setClearAlarmObject(List<String> clearAlarmObject) {
        this.clearAlarmObject = clearAlarmObject;

        sendClearAlarmNotification(this.clearAlarmObject);
    }

    private void sendClearAlarmNotification(List<String> alarmObject) {
        sendNotification(new AttributeChangeNotification(this, sequenceNumber.getAndIncrement(),
                System.currentTimeMillis(), "clear alarm object notified ", "clearAlarmObject", "ArrayList", "",
                alarmObject));
    }

    @Override
    public List<String> getClearAlarmObject() {
        return Collections.unmodifiableList(clearAlarmObject);
    }

    @Override
    public void raiseAlarm(String alarmName, String additionalText, String source) {
        sendRaiseAlarmNotification(ImmutableList.of(alarmName, additionalText, source));
    }

    @Override
    public void clearAlarm(String alarmName, String additionalText, String source) {
        sendClearAlarmNotification(ImmutableList.of(alarmName, additionalText, source));
    }
}

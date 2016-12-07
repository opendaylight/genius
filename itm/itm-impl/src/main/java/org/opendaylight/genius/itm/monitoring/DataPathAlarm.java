/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.monitoring;

import java.util.ArrayList;
import java.util.List;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationBroadcasterSupport;

/**
 * Implementation of the DataPath Alarm MBean. It can basically allow others to
 * rise and clear alarms occurred on the Data Path.
 *
 * @author Ericsson India Global Services Pvt Ltd. and others
 *
 */
public class DataPathAlarm extends NotificationBroadcasterSupport implements DataPathAlarmMBean {
    private long sequenceNumber = 1;

    private List<String> raiseAlarmObject = new ArrayList<>();
    private List<String> clearAlarmObject = new ArrayList<>();

    @Override
    public void setRaiseAlarmObject(List<String> raiseAlarmObject) {
        this.raiseAlarmObject = raiseAlarmObject;

        sendNotification(new AttributeChangeNotification(this, sequenceNumber++, System.currentTimeMillis(),
                "raise alarm object notified ", "raiseAlarmObject", "ArrayList", "", this.raiseAlarmObject));
    }

    @Override
    public List<String> getRaiseAlarmObject() {
        return raiseAlarmObject;
    }

    @Override
    public void setClearAlarmObject(List<String> clearAlarmObject) {
        this.clearAlarmObject = clearAlarmObject;
        sendNotification(new AttributeChangeNotification(this, sequenceNumber++, System.currentTimeMillis(),
                "clear alarm object notified ", "clearAlarmObject", "ArrayList", "", this.clearAlarmObject));
    }

    @Override
    public List<String> getClearAlarmObject() {
        return clearAlarmObject;
    }

    @Override
    public synchronized void raiseAlarm(String alarmName, String additionalText, String source) {
        raiseAlarmObject.add(alarmName);
        raiseAlarmObject.add(additionalText);
        raiseAlarmObject.add(source);
        setRaiseAlarmObject(raiseAlarmObject);
        raiseAlarmObject.clear();
    }

    @Override
    public synchronized void clearAlarm(String alarmName, String additionalText, String source) {
        clearAlarmObject.add(alarmName);
        clearAlarmObject.add(additionalText);
        clearAlarmObject.add(source);
        setClearAlarmObject(clearAlarmObject);
        clearAlarmObject.clear();
    }
}

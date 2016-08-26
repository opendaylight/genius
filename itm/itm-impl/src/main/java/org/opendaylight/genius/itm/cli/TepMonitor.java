/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "monitor-interval", description = "configuring tunnel monitoring time interval")
public class TepMonitor extends OsgiCommandSupport {

  @Argument(index = 0, name = "interval", description = "monitoring interval", required = true,
          multiValued = false)
  private Integer interval;

  private static final Logger logger = LoggerFactory.getLogger(TepMonitor.class);
  private IITMProvider itmProvider;

  public void setItmProvider(IITMProvider itmProvider) {
    this.itmProvider = itmProvider;
  }

  @Override
  protected Object doExecute() {
    try {
      logger.debug("Executing TEP monitor command with interval: " + "\t" + interval);
      if(!(interval >=ITMConstants.MIN_MONITOR_INTERVAL && interval<=ITMConstants.MAX_MONITOR_INTERVAL)){
        session.getConsole().println("Monitoring Interval must be in the range 100 - 30000");
      }
      else {
        itmProvider.configureTunnelMonitorInterval(interval);
      }
    } catch (Exception e) {
      throw e;
    }
    return null;
  }
}

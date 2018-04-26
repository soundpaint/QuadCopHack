/*
 * @(#)SerialReader.java 1.00 17/01/26
 *
 * Copyright (C) 2017 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.soundpaint.qcapp;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SerialReader extends Thread
{
  private final InputStream in;
  private final List<QCRecorder> recorders;
  private boolean running;

  private SerialReader()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public SerialReader(final InputStream in)
  {
    if (in == null) {
      throw new NullPointerException("in");
    }
    this.in = in;
    recorders = new ArrayList<QCRecorder>();
    running = false;
  }

  public boolean addRecorder(final QCRecorder recorder)
  {
    return recorders.add(recorder);
  }

  public boolean removeRecorder(final QCRecorder recorder)
  {
    return recorders.remove(recorder);
  }

  public boolean isRunning()
  {
    return running;
  }

  private enum ScanStatus {
    UNSYNCHRONIZED,
    QC_STATUS_READ,
    QC_CTRL_LEVER_BYTE0_READ,
    QC_CTRL_LEVER_BYTE1_READ,
    QC_CTRL_LEVER_BYTE2_READ,
    QC_CTRL_LEVER_BYTE3_READ,
    QC_CTRL_LEVER_BYTE4_READ,
    QC_BUTTONS_READ
  }

  private ScanStatus scanStatus;
  private byte status;
  private byte ctrlLever0;
  private byte ctrlLever1;
  private byte ctrlLever2;
  private byte ctrlLever3;
  private byte buttons;

  private void handleByte(byte b) {
    switch (scanStatus) {
    case UNSYNCHRONIZED:
    case QC_BUTTONS_READ:
      if (b < 0) {
        status = b;
        scanStatus = ScanStatus.QC_STATUS_READ;
      } else {
        // still or newly lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      break;
    case QC_STATUS_READ:
      if (b >= 0) {
        ctrlLever0 = (byte)(b << 1);
        scanStatus = ScanStatus.QC_CTRL_LEVER_BYTE0_READ;
      } else {
        // lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      break;
    case QC_CTRL_LEVER_BYTE0_READ:
      if (b >= 0) {
        ctrlLever0 |= b >> 6;
        ctrlLever1 = (byte)(b << 2);
        scanStatus = ScanStatus.QC_CTRL_LEVER_BYTE1_READ;
      } else {
        // lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      break;
    case QC_CTRL_LEVER_BYTE1_READ:
      if (b >= 0) {
        ctrlLever1 |= b >> 5;
        ctrlLever2 = (byte)(b << 3);
        scanStatus = ScanStatus.QC_CTRL_LEVER_BYTE2_READ;
      } else {
        // lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      break;
    case QC_CTRL_LEVER_BYTE2_READ:
      if (b >= 0) {
        ctrlLever2 |= b >> 4;
        ctrlLever3 = (byte)(b << 4);
        scanStatus = ScanStatus.QC_CTRL_LEVER_BYTE3_READ;
      } else {
        // lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      break;
    case QC_CTRL_LEVER_BYTE3_READ:
      if (b >= 0) {
        ctrlLever3 |= b >> 3;
        scanStatus = ScanStatus.QC_CTRL_LEVER_BYTE4_READ;
      } else {
        // lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      break;
    case QC_CTRL_LEVER_BYTE4_READ:
      if (b >= 0) {
        buttons = b;
        scanStatus = ScanStatus.QC_BUTTONS_READ;
      } else {
        // lost synchronisation
        scanStatus = ScanStatus.UNSYNCHRONIZED;
      }
      recordReceived();
      break;
    default:
      scanStatus = ScanStatus.UNSYNCHRONIZED;
      break;
    }
  }

  private byte count = 0;

  private void recordReceived()
  {
    final QuadCop.DataRecord record =
      new QuadCop.DataRecord(status, ctrlLever0, ctrlLever1,
                             ctrlLever2, ctrlLever3, buttons);
    for (final QCRecorder recorder : recorders) {
      recorder.recordReceived(record);
    }
  }

  public void run()
  {
    running = true;
    System.out.println("enter read loop");
    scanStatus = ScanStatus.UNSYNCHRONIZED;
    final byte[] buffer = new byte[1024];
    int len;
    try {
      while ((len = in.read(buffer)) > -1) {
        for (int i = 0; i < len; i++) {
          handleByte(buffer[i]);
        }
        if (len == 0) {
          // reduce cpu load for active polling
          try {
            Thread.sleep(20);
          } catch (final InterruptedException ex) {
            // ignore
          }
        }
      }
    } catch (final IOException ex) {
      // TODO: Maybe try recovering from error.
      ex.printStackTrace();
    }
    System.out.println("exit read loop");
    running = false;
  }

  public void close() throws IOException
  {
    if (running) {
      throw new IOException("still running");
    }
    in.close();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

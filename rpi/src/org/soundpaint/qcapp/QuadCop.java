/*
 * @(#)QuadCop.java 1.00 17/01/27
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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.ArrayList;
import java.util.List;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class QuadCop
{
  public static class DataRecord
  {
    // TODO: Add timestamp for tracking exact time?
    private final static int BYTE_LENGTH = 6;
    private final byte status;
    private final byte ctrlLever0;
    private final byte ctrlLever1;
    private final byte ctrlLever2;
    private final byte ctrlLever3;
    private final byte buttons;

    public DataRecord()
    {
      this((byte)0x80, (byte)0x00, (byte)0x00,
           (byte)0x00, (byte)0x00, (byte)0x00);
    }

    public DataRecord(final byte status,
                      final byte ctrlLever0,
                      final byte ctrlLever1,
                      final byte ctrlLever2,
                      final byte ctrlLever3,
                      final byte buttons)
    {
      this.status = status;
      this.ctrlLever0 = ctrlLever0;
      this.ctrlLever1 = ctrlLever1;
      this.ctrlLever2 = ctrlLever2;
      this.ctrlLever3 = ctrlLever3;
      this.buttons = buttons;
    }

    public static int getByteLength()
    {
      return BYTE_LENGTH;
    }

    public byte getStatus()
    {
      return status;
    }

    public byte getCtrlLever0()
    {
      return ctrlLever0;
    }

    public byte getCtrlLever1()
    {
      return ctrlLever1;
    }

    public byte getCtrlLever2()
    {
      return ctrlLever2;
    }

    public byte getCtrlLever3()
    {
      return ctrlLever3;
    }

    public byte getButtons()
    {
      return buttons;
    }

    public String toString()
    {
      return
        "QC record: stat=" + status +
        ", lev0=" + ctrlLever0 +
        ", lev1=" + ctrlLever1 +
        ", lev2=" + ctrlLever2 +
        ", lev3=" + ctrlLever3 +
        ", knobs=" + buttons;
    }
  }

  final private PrintStream log;
  final private CommPortIdentifier portIdentifier;
  final private SerialPort serialPort;
  final private InputStream serialIn;
  final private OutputStream serialOut;
  final private SerialReader reader;
  final private SerialWriter writer;

  private QuadCop() {
    throw new RuntimeException("unsupported constructor");
  }

  private final static int BUFFER_SIZE = 2000;

  private QuadCop(final PrintStream log,
                  final CommPortIdentifier portIdentifier) throws IOException
  {
    this.log = log;
    if (portIdentifier == null) {
      throw new NullPointerException("portidentifier");
    }
    this.portIdentifier = portIdentifier;
    final CommPort commPort;
    try {
      commPort = portIdentifier.open(this.getClass().getName(), BUFFER_SIZE);
    } catch (final PortInUseException ex) {
      throw new IOException("serial port currently in use", ex);
    }
    if (!(commPort instanceof SerialPort)) {
      throw new IOException("port is not a serial port");
    }
    serialPort = (SerialPort)commPort;
    try {
      serialPort.setSerialPortParams(57600,
                                     SerialPort.DATABITS_8,
                                     SerialPort.STOPBITS_1,
                                     SerialPort.PARITY_NONE);
    } catch (final UnsupportedCommOperationException ex) {
      throw new IOException("failed configuring serial port", ex);
    }
    System.out.println("starting serial reader...");
    serialIn = serialPort.getInputStream();
    reader = new SerialReader(serialIn);
    reader.start();
    while (!reader.isRunning()) {
      try {
        Thread.sleep(100);
      } catch (final InterruptedException ex) {
      }
    }
    System.out.println("serial reader started");
    System.out.println("starting serial writer...");
    serialOut = serialPort.getOutputStream();
    writer = new SerialWriter(reader, serialOut);
    writer.start();
    while (!writer.isRunning()) {
      try {
        Thread.sleep(100);
      } catch (final InterruptedException ex) {
      }
    }
    System.out.println("serial writer started");
  }

  public void close() throws IOException
  {
    /*
    while (writer.isRunning()) {
      try {
        Thread.sleep(500);
      } catch (final InterruptedException ex) {
      }
    }
    */
    writer.close();
    /*
    while (reader.isRunning()) {
      try {
        Thread.sleep(500);
      } catch (final InterruptedException ex) {
      }
    }
    */
    reader.close();
  }

  private void log(final String message)
  {
    if (log != null) {
      log.println(message + "\n");
    }
  }

  public static QuadCop create(final PrintStream log) throws IOException
  {
    return create(log, (String)null);
  }

  public static QuadCop create(final PrintStream log, final String portName)
    throws IOException
  {
    final Enumeration<CommPortIdentifier> portEnum =
      CommPortIdentifier.getPortIdentifiers();
    CommPortIdentifier portIdentifier = null;
    while (portEnum.hasMoreElements()) {
      portIdentifier = portEnum.nextElement();
      if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
        if ((portName == null) || (portName.equals(portIdentifier.getName()))) {
          break;
        }
      }
    }
    if (portIdentifier == null) {
      if (portName != null) {
        throw new IOException("no such serial port found: " + portName);
      } else {
        throw new IOException("no serial port found");
      }
    }
    return new QuadCop(log, portIdentifier);
  }

  final private static String[] EMPTY_STRING_ARRAY = new String[0];

  public static String[] getAvailablePortNames()
  {
    final List<String> ports = new ArrayList<String>();
    final Enumeration<CommPortIdentifier> portEnum =
      CommPortIdentifier.getPortIdentifiers();
    while (portEnum.hasMoreElements()) {
      final CommPortIdentifier portIdentifier = portEnum.nextElement();
      if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
        ports.add(portIdentifier.getName());
      }
    }
    return ports.toArray(EMPTY_STRING_ARRAY);
  }

  public String getPortName() {
    return portIdentifier.getName();
  }

  public synchronized boolean addRecorder(final QCRecorder recorder)
  {
    return reader.addRecorder(recorder);
  }

  public synchronized boolean removeRecorder(final QCRecorder recorder)
  {
    return reader.removeRecorder(recorder);
  }

  public synchronized boolean addPlayer(final QCPlayer player)
  {
    return writer.addPlayer(player);
  }

  public synchronized boolean removePlayer(final QCPlayer player)
  {
    return writer.removePlayer(player);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

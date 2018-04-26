/*
 * @(#)SerialWriter.java 1.00 17/01/27
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SerialWriter extends Thread implements QCRecorder
{
  private final static int BUFFER_SIZE = 64;
  private final SerialReader reader;
  private final OutputStream out;
  private final List<QCPlayer> players;
  private int txWriteBufferLevel;
  private QuadCop.DataRecord[] clientBuffer;
  private int clientBufferSize;
  private QuadCop.DataRecord[] writeBuffer;
  private int writeBufferSize;
  private int writeBufferWriteIndex;
  private boolean running;
  private IOException starvationException;

  private SerialWriter()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public SerialWriter(final SerialReader reader, final OutputStream out)
  {
    if (reader == null) {
      throw new NullPointerException("reader");
    }
    this.reader = reader;
    if (out == null) {
      throw new NullPointerException("out");
    }
    this.out = out;
    players = new ArrayList<QCPlayer>();
    txWriteBufferLevel = 0;
    clientBuffer = new QuadCop.DataRecord[BUFFER_SIZE];
    clientBufferSize = 0;
    writeBuffer = new QuadCop.DataRecord[BUFFER_SIZE];
    writeBufferSize = 0;
    writeBufferWriteIndex = 0;
    running = false;
    starvationException = null;
  }

  public IOException getStarvationException()
  {
    return starvationException;
  }

  public void recordReceived(final QuadCop.DataRecord record)
  {
    // AtMega serial buffer size is 64 bytes
    txWriteBufferLevel = record.getStatus() & 0x3f;
  }

  private boolean txWriteBufferIsReady()
  {
    // do write only if serial write buffer is at most 50% (=32 bytes)
    // filled
    return txWriteBufferLevel <= 0x38;
  }

  public boolean addPlayer(final QCPlayer player)
  {
    if (players.size() > 0) {
      // signal mixer not yet implemented
      throw new RuntimeException("currently, at most one player " +
                                 "may connect to the QuadCop");
    }
    return players.add(player);
  }

  public boolean removePlayer(final QCPlayer player)
  {
    return players.remove(player);
  }

  public boolean isRunning()
  {
    return running;
  }

  private void bufferUnderrunDetected()
  {
    for (final QCPlayer player : players) {
      player.bufferUnderrunDetected();
    }
  }

  public void provideNextChunk()
  {
    synchronized(writeBuffer) {
      // support for multiple players (via signal mixer) not yet
      // implemented => just take the first one from the list
      if (!players.isEmpty()) {
        final QCPlayer player = players.get(0);
        clientBufferSize = player.provideNextChunk(clientBuffer);
      }
    }
  }

  private static final long RE_REPORT_WRITE_ONLY_AFTER_MS = 1000;
  private long lastWriteReported = 0;

  private void writeDataRecord(final QuadCop.DataRecord record)
    throws IOException
  {
    byte txByte;
    txByte = (byte)(record.getStatus() | 0x80);
    out.write(txByte);
    txByte = (byte)((record.getCtrlLever0() & 0xff) >> 1);
    out.write(txByte);
    txByte =
      (byte)(((record.getCtrlLever0() << 6) |
              ((record.getCtrlLever1() & 0xff) >> 2)) & 0x7f);
    out.write(txByte);
    txByte =
      (byte)(((record.getCtrlLever1() << 5) |
              ((record.getCtrlLever2() & 0xff) >> 3)) & 0x7f);
    out.write(txByte);
    txByte =
      (byte)(((record.getCtrlLever2() << 4) |
              ((record.getCtrlLever3() & 0xff) >> 4)) & 0x7f);
    out.write(txByte);
    txByte = (byte)((record.getCtrlLever3() << 3) & 0x7f);
    out.write(txByte);
    txByte = (byte)(record.getButtons() & 0x7f);
    out.write(txByte);
    out.flush();
    final long writeReported = System.currentTimeMillis();
    if (writeReported - lastWriteReported > RE_REPORT_WRITE_ONLY_AFTER_MS) {
      System.err.println("*** " + writeReported + " wrote record ***");
      lastWriteReported = writeReported;
    }
  }

  private static final long RE_REPORT_BUFFER_UNDERRUN_ONLY_AFTER_MS = 1000;
  private long lastBufferUnderrunReported = 0;

  private void swapBuffers()
  {
    synchronized(writeBuffer) {
      if (clientBufferSize > 0) {
        final QuadCop.DataRecord[] swapBuffer = writeBuffer;
        writeBuffer = clientBuffer;
        writeBufferSize = clientBufferSize;
        clientBuffer = swapBuffer;
        clientBufferSize = 0;
      } else {
        bufferUnderrunDetected();
        final long bufferUnderrunReported = System.currentTimeMillis();
        if (bufferUnderrunReported - lastBufferUnderrunReported >
            RE_REPORT_BUFFER_UNDERRUN_ONLY_AFTER_MS) {
          System.err.println("*** serial writer: buffer underrun ***");
          lastBufferUnderrunReported = bufferUnderrunReported;
        }
      }
    }
  }

  private static class Producer extends Thread
  {
    private final SerialWriter serialWriter;
    private boolean stopRequested = false;
    private boolean haveNeed = false;

    private Producer()
    {
      throw new RuntimeException("unsupported constructor");
    }

    public Producer(final SerialWriter serialWriter)
    {
      this.serialWriter = serialWriter;
      stopRequested = false;
    }

    private synchronized void requireNextChunk()
    {
      //System.err.println("req. next chunk");
      while (haveNeed) {
        try {
          wait();
        } catch (final InterruptedException ex) {
          // ignore
        }
      }
      haveNeed = true;
      notifyAll();
    }

    private synchronized void satisfyNextChunk()
    {
      //System.err.println("sat. next chunk");
      while (!haveNeed) {
        try {
          wait();
        } catch (final InterruptedException ex) {
          // ignore
        }
      }
      serialWriter.provideNextChunk();
      haveNeed = false;
      notifyAll();
    }

    public void run()
    {
      while (!stopRequested) {
        satisfyNextChunk();
      }
      stopRequested = false;
    }

    public void requestStop()
    {
      stopRequested = true;
    }
  }

  private void pause(final int millis)
  {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException ex) {
      // ignore
    }
  }

  public void run()
  {
    running = true;
    System.out.println("enter write loop");
    reader.addRecorder(this);
    final Producer producer = new Producer(this);
    producer.start();
    producer.requireNextChunk();
    swapBuffers();
    System.out.println("pre-loaded " + writeBufferSize + " records");
    while (starvationException == null) {
      try {
        if (writeBufferSize > 0) {
          for (int i = 0; i < writeBufferSize; i++) {
            while (!txWriteBufferIsReady()) {
              // AtMega requires ~5 ms to process half of the tx
              // buffer, so keep below that
              pause(1);
            }
            writeDataRecord(writeBuffer[i]);
            if (i == (writeBufferSize / 2)) {
              producer.requireNextChunk();
            }
          }
        } else {
          // empty write buffer => must wait for producer anyway
          producer.requireNextChunk();
          pause(1);
        }
        writeBufferSize = 0;
        swapBuffers();
      } catch (final IOException ex) {
        starvationException = ex;
      }
    }
    producer.requestStop();
    reader.removeRecorder(this);
    System.out.println("exit write loop: " + starvationException.getMessage());
    running = false;
  }

  public void close() throws IOException
  {
    if (running) {
      throw new IOException("still running");
    }
    out.close();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

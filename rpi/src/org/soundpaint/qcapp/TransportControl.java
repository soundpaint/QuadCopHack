/*
 * @(#)TransportControl.java 1.00 17/01/27
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
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class TransportControl
{
  public static interface Listener extends EventListener
  {
    public void statusChanged(final Status oldStatus,
                              final Status newStatus);
    public void playProgressChanged(final double progress,
                                    final String progressAsPercent,
                                    final int index,
                                    final int size);
    public void recordProgressChanged(final int size);
  }

  public enum Status {
    STOPPED,
    PLAYING,
    RECORDING
  };

  private final QuadCop quadCop;
  private final List<Listener> listeners;
  private Status status;
  DocumentPlayer player;
  DocumentRecorder recorder;

  private TransportControl()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public TransportControl(final QuadCop quadCop)
  {
    this.quadCop = quadCop;
    listeners = new ArrayList<Listener>();
    status = Status.STOPPED;
    player = null;
    recorder = null;
  }

  public boolean addListener(final Listener listener)
  {
    return listeners.add(listener);
  }

  public boolean removeListener(final Listener listener)
  {
    return listeners.remove(listener);
  }

  public boolean canPlay()
  {
    // return false if e.g. no serial port available
    return true;
  }

  public boolean canRecord()
  {
    // return false if e.g. no serial port available
    return true;
  }

  public synchronized void stop() {
    if ((status != Status.PLAYING) &&
        (status != Status.RECORDING)) {
      throw new IllegalStateException("not playing or recording");
    }
    if (recorder != null) {
      if (quadCop != null) {
        quadCop.removeRecorder(recorder);
      }
      recorder = null;
    } else if (player != null) {
      if (quadCop != null) {
        quadCop.removePlayer(player);
      }
      player = null;
    } else {
      throw new IllegalStateException("neither player nor recorder present");
    }
    changeStatus(Status.STOPPED);
  }

  public synchronized void play(final Document document) {
    if (status != Status.STOPPED) {
      throw new IllegalStateException("not stopped");
    }
    if (player != null) {
      throw new IllegalStateException("player already present");
    }
    player = new DocumentPlayer(document);
    player.addProgressListener(new QCPlayer.ProgressListener() {
        public void endOfStreamReached() {
          stop();
        }
        public void progressChanged(final double progress,
                                    final String progressAsPercent,
                                    final int index,
                                    final int size)
        {
          TransportControl.this.
            playProgressChanged(progress, progressAsPercent, index, size);
        }
      });
    if (quadCop != null) {
      quadCop.addPlayer(player);
    }
    changeStatus(Status.PLAYING);
  }

  public synchronized void record(final Document document) {
    if (status != Status.STOPPED) {
      throw new IllegalStateException("not stopped");
    }
    if (recorder != null) {
      throw new IllegalStateException("recorder already present");
    }
    recorder = new DocumentRecorder(document);
    recorder.addProgressListener(new QCRecorder.ProgressListener() {
        public void progressChanged(final int size)
        {
          TransportControl.this.recordProgressChanged(size);
        }
      });
    if (quadCop != null) {
      quadCop.addRecorder(recorder);
    }
    changeStatus(Status.RECORDING);
  }

  private void changeStatus(final Status newStatus)
  {
    final Status oldStatus = status;
    status = newStatus;
    for (final Listener listener : listeners) {
      listener.statusChanged(oldStatus, newStatus);
    }
  }

  public void playProgressChanged(final double progress,
                                  final String progressAsPercent,
                                  final int index,
                                  final int size)
  {
    for (final Listener listener : listeners) {
      listener.playProgressChanged(progress, progressAsPercent, index, size);
    }
  }

  public void recordProgressChanged(final int size)
  {
    for (final Listener listener : listeners) {
      listener.recordProgressChanged(size);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

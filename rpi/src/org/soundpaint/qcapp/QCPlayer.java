/*
 * @(#)QCPlayer.java 1.00 17/01/26
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

import java.util.EventListener;

public interface QCPlayer extends EventListener
{
  public static interface ProgressListener extends EventListener
  {
    public void endOfStreamReached();

    public void progressChanged(final double progress,
                                final String progressAsPercent,
                                final int index,
                                final int size);
  }

  public boolean addProgressListener(final ProgressListener listener);

  public boolean removeProgressListener(final ProgressListener listener);

  /**
   * If this method is called, the player must provide its next chunk
   * of data and store it in the provided buffer.  The player may fill
   * the buffer only partially from array field 0 to up to array field
   * N - 1 (with N being at most the array size of the buffer).  The
   * client must return this number N, thus indicating the number of
   * provided array fields N.
   */
  public int provideNextChunk(final QuadCop.DataRecord[] buffer);

  /**
   * Notifies the client that a buffer underrun had occurred.
   */
  public void bufferUnderrunDetected();
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

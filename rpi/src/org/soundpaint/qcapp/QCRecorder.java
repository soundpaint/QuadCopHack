/*
 * @(#)QCRecorder.java 1.00 17/01/26
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

public interface QCRecorder extends EventListener
{
  public static interface ProgressListener extends EventListener
  {
    public void progressChanged(final int size);
  }

  public void recordReceived(final QuadCop.DataRecord record);
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

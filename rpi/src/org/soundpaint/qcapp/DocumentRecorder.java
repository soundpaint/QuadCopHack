/*
 * @(#)DocumentRecorder.java 1.00 17/01/27
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

import java.util.ArrayList;
import java.util.List;

public class DocumentRecorder implements QCRecorder
{
  private final Document document;
  private final List<ProgressListener> progressListeners;

  private DocumentRecorder()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public DocumentRecorder(final Document document)
  {
    if (document == null) {
      throw new NullPointerException("document");
    }
    this.document = document;
    progressListeners = new ArrayList<ProgressListener>();
  }

  public boolean addProgressListener(final ProgressListener progressListener)
  {
    final boolean success = progressListeners.add(progressListener);
    progressListener.progressChanged(document.size());
    return success;
  }

  public boolean removeProgressListener(final ProgressListener progressListener)
  {
    return progressListeners.remove(progressListener);
  }

  private void progressChanged()
  {
    for (final ProgressListener progressListener : progressListeners) {
      progressListener.progressChanged(document.size());
    }
  }

  public void recordReceived(final QuadCop.DataRecord record)
  {
    document.addRecord(record);
    progressChanged();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

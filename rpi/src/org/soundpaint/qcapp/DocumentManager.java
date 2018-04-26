/*
 * @(#)DocumentManager.java 1.00 17/01/25
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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class DocumentManager implements Document.Listener
{
  public static interface Listener extends EventListener
  {
    public void statusChanged(final Status status,
                              final int documentSize);
    public void fileAssociationChanged(final File file);
  }

  public enum Status {
    EMPTY_UNNAMED_DOCUMENT,
    MODIFIED_UNNAMED_DOCUMENT,
    MODIFIED_NAMED_DOCUMENT,
    UNMODIFIED_NAMED_DOCUMENT
  };

  private final static FileFilter DEFAULT_FILE_FILTER =
    new FileNameExtensionFilter("Quad Cop Recorder Files", "rec");

  private final QuadCopApp quadCopApp;
  private final JFileChooser fileChooser;
  private final List<Listener> listeners;
  private Status status;
  private File file;
  private Document document;
  private int documentSize;

  private DocumentManager()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public DocumentManager(final QuadCopApp quadCopApp)
  {
    if (quadCopApp == null) {
      throw new NullPointerException("quadCopApp");
    }
    this.quadCopApp = quadCopApp;
    fileChooser = new JFileChooser();
    fileChooser.setFileFilter(DEFAULT_FILE_FILTER);
    fileChooser.setCurrentDirectory(new File("").getAbsoluteFile());
    listeners = new ArrayList<Listener>();
    status = Status.EMPTY_UNNAMED_DOCUMENT;
    file = null;
    document = null;
  }

  public Document getDocument()
  {
    if (document == null) {
      throw new NullPointerException("document");
    }
    return document;
  }

  public boolean addListener(final Listener listener)
  {
    return listeners.add(listener);
  }

  public boolean removeListener(final Listener listener)
  {
    return listeners.remove(listener);
  }

  public Status getStatus()
  {
    return status;
  }

  public void updateStatus(final Status status)
  {
    final int documentSize = document != null ? document.size() : 0;
    final boolean statusChanged =
      (this.status != status) || (this.documentSize != documentSize);
    this.status = status;
    this.documentSize = documentSize;
    if (statusChanged) {
      for (final Listener listener : listeners) {
        listener.statusChanged(status, documentSize);
      }
    }
  }

  public boolean isDocumentEmpty()
  {
    return status == Status.EMPTY_UNNAMED_DOCUMENT;
  }

  public boolean isModified()
  {
    return
      (status == Status.MODIFIED_UNNAMED_DOCUMENT) ||
      (status == Status.MODIFIED_NAMED_DOCUMENT);
  }

  public void documentChanged()
  {
    if (status == Status.EMPTY_UNNAMED_DOCUMENT) {
      updateStatus(Status.MODIFIED_UNNAMED_DOCUMENT);
    } else if (status == Status.UNMODIFIED_NAMED_DOCUMENT) {
      updateStatus(Status.MODIFIED_NAMED_DOCUMENT);
    } else {
      // document already modified => nothing to do
    }
  }

  private void updateFileAssociation(final File file)
  {
    final boolean fileAssociationChanged = this.file != file;
    this.file = file;
    if (fileAssociationChanged) {
      for (final Listener listener : listeners) {
        listener.fileAssociationChanged(file);
      }
    }
  }

  public void open()
  {
    if ((status == Status.MODIFIED_UNNAMED_DOCUMENT) ||
        (status == Status.MODIFIED_NAMED_DOCUMENT)) {
      if (confirmDiscardChanges("Open File")) {
        clearDocument();
        doOpen();
      } else {
        // abort open
      }
    } else {
      doOpen();
    }
  }

  private void doOpen()
  {
    final int selectedOption = fileChooser.showOpenDialog(quadCopApp);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      final File file = fileChooser.getSelectedFile();
      loadDocument(file);
    }
  }

  public void save()
  {
    if (status == Status.MODIFIED_UNNAMED_DOCUMENT) {
      saveAs();
    } else if (status == Status.MODIFIED_NAMED_DOCUMENT) {
      saveDocument(file);
    } else {
      // empty or unmodified file => nothing to do
    }
  }

  public void saveAs()
  {
    if (status != Status.EMPTY_UNNAMED_DOCUMENT) {
      final int selectedOption = fileChooser.showSaveDialog(quadCopApp);
      if (selectedOption == JFileChooser.APPROVE_OPTION) {
        final File file = fileChooser.getSelectedFile();
        saveDocumentAs(file);
      }
    } else {
      // empty file => nothing to do
    }
  }

  public void close()
  {
    if (status == Status.MODIFIED_NAMED_DOCUMENT ||
        status == Status.MODIFIED_UNNAMED_DOCUMENT) {
      if (confirmDiscardChanges("Close File")) {
        clearDocument();
      }
    } else if (status == Status.UNMODIFIED_NAMED_DOCUMENT) {
      clearDocument();
    } else {
      // empty file => nothing to do
    }
  }

  private boolean confirmDiscardChanges(final String title)
  {
    final int selectedOption =
      JOptionPane.showConfirmDialog(quadCopApp,
                                    "Discard changes?",
                                    title,
                                    JOptionPane.YES_NO_OPTION);
    return selectedOption == JOptionPane.YES_OPTION;
  }

  private void confirmOpenFailed(final String message)
  {
    JOptionPane.showMessageDialog(quadCopApp,
                                  message,
                                  "Failed Opening File",
                                  JOptionPane.ERROR_MESSAGE);
  }

  private boolean loadDocument(final File file)
  {
    if (document != null) {
      throw new IllegalStateException("overwriting existing document");
    }
    boolean success;
    String message;
    try {
      document = Document.createFromFile(file);
      success = true;
      message = null;
    } catch (final IOException ex) {
      success = false;
      message = ex.getMessage();
    }
    if (success) {
      updateStatus(Status.UNMODIFIED_NAMED_DOCUMENT);
      updateFileAssociation(file);
    } else {
      confirmOpenFailed(message);
    }
    return success;
  }

  private void confirmSaveFailed(final String message)
  {
    JOptionPane.showMessageDialog(quadCopApp,
                                  message,
                                  "Failed Saving File",
                                  JOptionPane.ERROR_MESSAGE);
  }

  private boolean saveDocument(final File file)
  {
    if (document == null) {
      throw new NullPointerException("document");
    }
    boolean success = true;
    String message;
    try {
      document.saveToFile(file);
      success = true;
      message = null;
    } catch (final IOException ex) {
      success = false;
      message = ex.getMessage();
    }
    if (success) {
      updateStatus(Status.UNMODIFIED_NAMED_DOCUMENT);
    } else {
      confirmSaveFailed(message);
    }
    return success;
  }

  private void saveDocumentAs(final File file)
  {
    if (saveDocument(file)) {
      updateFileAssociation(file);
    } else {
      // keep status unmodified
    }
  }

  private void clearDocument()
  {
    if (document == null) {
      throw new IllegalArgumentException("document already cleared");
    }
    document = null;
    updateFileAssociation(null);
    updateStatus(Status.EMPTY_UNNAMED_DOCUMENT);
  }

  public Document newDocument()
  {
    if (document != null) {
      if (confirmDiscardChanges("Record Data")) {
        document.clear();
        if (status == Status.UNMODIFIED_NAMED_DOCUMENT) {
          updateStatus(Status.MODIFIED_NAMED_DOCUMENT);
        } else if (status == Status.MODIFIED_UNNAMED_DOCUMENT) {
          // keep modified
        } else if (status == Status.MODIFIED_NAMED_DOCUMENT) {
          // keep modified
        } else {
          throw new IllegalStateException("unexpected status " + status);
        }
        return document;
      } else {
        // do not touch document
        return null;
      }
    } else {
      document = Document.createNew();
      updateStatus(Status.MODIFIED_UNNAMED_DOCUMENT);
      return document;
    }
  }

  private void addRecord(final QuadCop.DataRecord record)
  {
    if (document == null) {
      throw new NullPointerException("document");
    }
    document.addRecord(record);
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

/*
 * @(#)QuadCopApp.java 1.00 17/01/25
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

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class QuadCopApp extends JFrame implements DocumentManager.Listener
{
  private static final long serialVersionUID = 4606585754005214101L;

  private QuadCop quadCop;
  private boolean haveUnsavedChanges;
  private String fileName;
  private final TransportControl transportControl;
  private final DocumentManager documentManager;
  private final QuadCopPane quadCopPane;

  public QuadCopApp()
  {
    addWindowListener(new WindowAdapter() {
        public void windowClosing(final WindowEvent e)
        {
          quit();
        }
      });
    haveUnsavedChanges = false;
    fileName = null;
    updateTitle();
    try {
      quadCop = QuadCop.create(System.out, null);
    } catch (final IOException ex) {
      quadCop = null;
      final String title = "could not connect to quad copter";
      final String msg = ex.getMessage();
      JOptionPane.showMessageDialog(this, msg, title,
                                    JOptionPane.ERROR_MESSAGE);
    }

    transportControl = new TransportControl(quadCop);
    documentManager = new DocumentManager(this);
    documentManager.addListener(this);

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setJMenuBar(new MenuBar(this));
    add(new ToolBar(this), BorderLayout.PAGE_START);
    add(new StatusLine(this), BorderLayout.PAGE_END);

    quadCopPane = new QuadCopPane(this);
    add(quadCopPane, BorderLayout.CENTER);

    pack();
    setVisible(true);

    if (quadCop != null) {
      quadCop.addRecorder(quadCopPane);
    }
  }

  private boolean confirmDiscardChanges()
  {
    final int selectedOption =
      JOptionPane.showConfirmDialog(this,
                                    "Discard changes?",
                                    "Quit Application",
                                    JOptionPane.YES_NO_OPTION);
    return selectedOption == JOptionPane.YES_OPTION;
  }

  public void quit()
  {
    
    if (!haveUnsavedChanges || confirmDiscardChanges()) {
      System.exit(0);
    } else {
      // abort quit
    }
  }

  public String getDescription()
  {
    return
      "QuadCopApp V0.1\n" +
      "\n" +
      "© 2017, 2018 by J. Reuter\n" +
      "Karlsruhe, Germany\n";
  }

  public TransportControl getTransportControl()
  {
    return transportControl;
  }

  public DocumentManager getDocumentManager()
  {
    return documentManager;
  }

  private static void createAndShowGUI()
  {
    final QuadCopApp quadCopApp = new QuadCopApp();
  }

  private final static String APP_TITLE = "QuadCop App";

  private void updateTitle()
  {
    final StringBuffer title = new StringBuffer();
    title.append(APP_TITLE);
    if ((fileName != null) || haveUnsavedChanges) {
      title.append(" (");
    }
    if (fileName != null) {
      title.append(fileName);
    }
    if (haveUnsavedChanges) {
      title.append("*");
    }
    if ((fileName != null) || haveUnsavedChanges) {
      title.append(")");
    }
    setTitle(title.toString());
  }

  public void statusChanged(final DocumentManager.Status status,
                            final int documentSize)
  {
    switch (status) {
    case MODIFIED_UNNAMED_DOCUMENT:
    case MODIFIED_NAMED_DOCUMENT:
      haveUnsavedChanges = true;
      break;
    default:
      haveUnsavedChanges = false;
    }
    updateTitle();
  }

  public void fileAssociationChanged(final File file)
  {
    if (file != null) {
      fileName = file.getName();
    } else {
      fileName = null;
    }
    updateTitle();
  }

  public static void main(final String argv[])
  {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          createAndShowGUI();
        }
      });
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */

/*
 * Copyright 2019 Nikita Shakarun
 * Licensed under the Apache License, Version 2.0.
 * Source: https://github.com/nikita36078/J2ME-Loader
 */
package javax.microedition.media.control;

import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.media.Control;
import javax.microedition.media.MediaException;

/** Compile-time JSR-135 API stub. It is not packaged in the MIDlet. */
public interface RecordControl extends Control {
  void setRecordStream(OutputStream stream);

  void setRecordLocation(String locator) throws IOException, MediaException;

  String getContentType();

  void startRecord();

  void stopRecord();

  void commit() throws IOException;

  int setRecordSizeLimit(int size) throws MediaException;

  void reset() throws IOException;
}

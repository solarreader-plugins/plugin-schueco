/*
 * Copyright (c) 2024-2025 Stefan Toengi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.schnippsche.solarreader.test;

import de.schnippsche.solarreader.backend.connection.usb.UsbConnection;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import org.tinylog.Logger;

public class SchuecoUsbConnection implements UsbConnection {

  private String result = null;
  private boolean open = false;

  @Override
  public void connect() throws ConnectException {
    Logger.debug("openPort");
    if (open) {
      throw new ConnectException("Port already in use");
    }
    open = true;
  }

  /**
   * Reads the next byte of data. The value byte is returned as an int in the range 0 to 255. If no
   * byte is available because the end of the stream has been reached, the value -1 is returned.
   * This method blocks until input data is available, the end of the stream is detected, or an
   * exception is thrown.
   *
   * @return the next byte from 0 to 255 or -1
   */
  @Override
  public int readByte() {
    if (!result.isEmpty()) {
      char firstChar = result.charAt(0);
      byte firstByte = (byte) firstChar;
      result = result.substring(1);
      return firstByte;
    }
    return '\r';
  }

  @Override
  public int writeBytes(byte[] bytes) throws IOException {
    assert (bytes != null);
    String productField = "#019\r";
    if (Arrays.equals(bytes, productField.getBytes())) {
      result = "\n*019 SG3502 h\r";
      return result.length();
    }
    String infoField = "#010\r";
    if (Arrays.equals(bytes, infoField.getBytes())) {
      result = "\n*010   4 350.0  1.18   414 229.2  1.74   398  31   1139 x\r";
      return result.length();
    }
    Logger.error("unknown command: {}", new String(bytes));
    throw new IOException("unknown command");
  }

  @Override
  public void disconnect() {
    Logger.debug("closePort");
    if (open) {
      open = false;
    } else throw new RuntimeException("closed port without open");
  }
}

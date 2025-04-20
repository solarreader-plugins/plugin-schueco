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
package de.schnippsche.solarreader.plugins.schueco;

import de.schnippsche.solarreader.backend.calculator.StringArrayCalculator;
import de.schnippsche.solarreader.backend.connection.general.ConnectionFactory;
import de.schnippsche.solarreader.backend.connection.usb.UsbConnection;
import de.schnippsche.solarreader.backend.connection.usb.UsbConnectionFactory;
import de.schnippsche.solarreader.backend.frame.KacoFrame;
import de.schnippsche.solarreader.backend.protocol.KacoProtocol;
import de.schnippsche.solarreader.backend.protocol.KnownProtocol;
import de.schnippsche.solarreader.backend.protocol.Protocol;
import de.schnippsche.solarreader.backend.provider.AbstractUsbProvider;
import de.schnippsche.solarreader.backend.provider.CommandProviderProperty;
import de.schnippsche.solarreader.backend.provider.ProviderInterface;
import de.schnippsche.solarreader.backend.provider.ProviderProperty;
import de.schnippsche.solarreader.backend.provider.SupportedInterface;
import de.schnippsche.solarreader.backend.table.Table;
import de.schnippsche.solarreader.backend.util.SerialPortConfigurationBuilder;
import de.schnippsche.solarreader.backend.util.Setting;
import de.schnippsche.solarreader.backend.util.TimeEvent;
import de.schnippsche.solarreader.database.Activity;
import de.schnippsche.solarreader.frontend.ui.HtmlInputType;
import de.schnippsche.solarreader.frontend.ui.HtmlWidth;
import de.schnippsche.solarreader.frontend.ui.UIInputElementBuilder;
import de.schnippsche.solarreader.frontend.ui.UIList;
import de.schnippsche.solarreader.plugin.PluginMetadata;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.tinylog.Logger;

/**
 * The Schueco class is a plugin that provides support for Schueco devices. It implements the {@link
 * ProviderInterface} and manages communication with the device through USB connections.
 *
 * <p>This plugin is configured with metadata using the {@link PluginMetadata} annotation, which
 * provides information such as the plugin's name, version, author, and supported interfaces.
 */
@PluginMetadata(
    name = "Schueco",
    version = "1.0.1",
    author = "Stefan Töngi",
    url = "https://github.com/solarreader-plugins/plugin-Schueco",
    svgImage = "schueco.svg",
    supportedInterfaces = {SupportedInterface.NAMED_USB, SupportedInterface.LISTED_USB},
    usedProtocol = KnownProtocol.KACO,
    supports = "Schueco SGI")
public class Schueco extends AbstractUsbProvider {
  private final Protocol<KacoFrame> protocol;
  private final StringArrayCalculator calculator;
  private int address;

  /**
   * Default constructor that initializes the Schueco plugin with a default USB connection factory.
   */
  public Schueco() {
    this(new UsbConnectionFactory());
  }

  /**
   * Constructor that allows for the injection of a custom USB connection factory.
   *
   * @param connectionFactory The connection factory to be used for creating USB connections.
   */
  public Schueco(ConnectionFactory<UsbConnection> connectionFactory) {
    super(connectionFactory);
    this.protocol = new KacoProtocol();
    this.calculator = new StringArrayCalculator();
    Logger.debug("instantiate {}", this.getClass().getName());
  }

  /**
   * Retrieves the resource bundle for the plugin based on the specified locale.
   *
   * <p>This method overrides the default implementation to return a {@link ResourceBundle} for the
   * plugin using the provided locale.
   *
   * @return The {@link ResourceBundle} for the plugin, localized according to the specified locale.
   */
  @Override
  public ResourceBundle getPluginResourceBundle() {
    return ResourceBundle.getBundle("schueco", locale);
  }

  /**
   * Retrieves the default activity for this plugin, which defines a schedule based on sunrise and
   * sunset events.
   *
   * @return The default activity object.
   */
  @Override
  public Activity getDefaultActivity() {
    return new Activity(TimeEvent.SUNRISE, -60, TimeEvent.SUNSET, 3600, 60, TimeUnit.SECONDS);
  }

  /**
   * Provides the dialog UI elements for configuring the provider, based on the locale.
   *
   * @return An optional UIList containing the dialog elements, or an empty optional if not
   *     applicable.
   */
  @Override
  public Optional<UIList> getProviderDialog() {
    UIList uiList = new UIList();
    uiList.addElement(
        new UIInputElementBuilder()
            .withId("id-address")
            .withRequired(true)
            .withType(HtmlInputType.TEXT)
            .withColumnWidth(HtmlWidth.HALF)
            .withLabel(resourceBundle.getString("schueco.address.text"))
            .withName(Setting.PROVIDER_ADDRESS)
            .withPlaceholder(resourceBundle.getString("schueco.address.text"))
            .withTooltip(resourceBundle.getString("schueco.address.tooltip"))
            .withInvalidFeedback(resourceBundle.getString("schueco.address.error"))
            .build());

    return Optional.of(uiList);
  }

  /**
   * Retrieves the supported properties for this provider from the "schueco.json" resource file.
   *
   * @return An optional list of supported properties, or an empty optional if the properties cannot
   *     be loaded.
   */
  @Override
  public Optional<List<ProviderProperty>> getSupportedProperties() {
    return getSupportedPropertiesFromFile("schueco_fields.json");
  }

  /**
   * Retrieves the default tables for this provider, which define how data is displayed.
   *
   * @return An optional list of default tables, or an empty optional if not applicable.
   */
  @Override
  public Optional<List<Table>> getDefaultTables() {
    return getDefaultTablesFromFile("schueco_tables.json");
  }

  /**
   * Retrieves the default provider setting, which includes serial port configuration with a baud
   * rate of 9600.
   *
   * @return The default setting object.
   */
  @Override
  public Setting getDefaultProviderSetting() {
    return new SerialPortConfigurationBuilder().withBaudrate(9600).withProviderAddress(1).build();
  }

  /**
   * Tests the provider connection using the provided test settings. Sends and receives data to
   * validate the connection.
   *
   * @param testSetting The settings to use for testing the connection.
   * @return A success message if the connection is valid, otherwise throws an IOException.
   * @throws IOException If the connection or data transfer fails.
   */
  @Override
  public String testProviderConnection(Setting testSetting) throws IOException {
    KacoFrame testFrame = new KacoFrame(testSetting.getProviderAddress(), '9');
    Logger.debug(testSetting);
    try (UsbConnection testUsbConnection = connectionFactory.createConnection(testSetting)) {
      testUsbConnection.connect();
      protocol.sendData(testUsbConnection, testFrame);
      testFrame = protocol.receiveData(testUsbConnection);
      if (testFrame.isValid()) {
        String message = resourceBundle.getString("schueco.connection.successful");
        return MessageFormat.format(message, testFrame.getData()[0]);
      }
      throw new IOException(resourceBundle.getString("schueco.connection.error"));
    } catch (Exception e) {
      throw new IOException(resourceBundle.getString("schueco.connection.error"));
    }
  }

  /**
   * Executes actions to be performed on the first run of the provider. Initializes settings,
   * properties, and tables.
   */
  @Override
  public void doOnFirstRun() {
    doStandardFirstRun();
  }

  /**
   * Performs the work associated with the defined activity, including opening the USB connection
   * and processing properties.
   *
   * @param variables A map of variables to be used during activity work.
   * @return true if the activity work is completed successfully.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public boolean doActivityWork(Map<String, Object> variables)
      throws IOException, InterruptedException {
    try (UsbConnection connection = getConnection()) {
      connection.connect();
      address = providerData.getSetting().getProviderAddress();
      workProperties(connection, variables);
      return true;
    }
  }

  @Override
  protected void handleCommandProperty(
      UsbConnection usbConnection,
      CommandProviderProperty commandProviderProperty,
      Map<String, Object> variables)
      throws IOException {
    String command = commandProviderProperty.getCommand();
    KacoFrame kacoFrame = new KacoFrame(address, command.charAt(0));
    Logger.debug("send command {} to address {}", command, address);
    protocol.sendData(usbConnection, kacoFrame);
    kacoFrame = protocol.receiveData(usbConnection);
    Logger.debug(kacoFrame);
    if (!kacoFrame.isValid()) {
      Logger.error("invalid frame, address:{}, command:{}", address, command);
    } else {
      commandProviderProperty.setCachedValue(kacoFrame.getData());
      calculator.calculate(
          kacoFrame.getData(), commandProviderProperty.getPropertyFieldList(), variables);
    }
  }

  @Override
  protected void handleCachedCommandProperty(
      UsbConnection connection,
      CommandProviderProperty commandProviderProperty,
      Map<String, Object> variables) {
    String[] cachedValue = (String[]) commandProviderProperty.getCachedValue();
    Logger.debug("use cached value {}", Arrays.toString(cachedValue));
    calculator.calculate(cachedValue, commandProviderProperty.getPropertyFieldList(), variables);
  }
}

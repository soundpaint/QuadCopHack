/* QuadCop Control Interface
 *
 * (C) 2017 by J. Reuter, Karlsruhe, Germany
 */

//#define MIXED_BUTTONS 1

#define bit_t byte

const byte OFF = LOW;
const byte ON = HIGH;
const byte RELEASED = LOW;
const byte PRESSED = HIGH;

const int ctrlLevers = 4;
const int buttons = 6;

const byte arduinoActivityLEDPin = 13;
const byte demuxValBasePin = 26;
const byte demuxAdrBasePin = 36;
const byte demuxEnablePin = 39;
const byte ctrlLeverInPin[ctrlLevers] = { A3, A2, A0, A1 };
const byte ctrlLeverOutPin[ctrlLevers] = { 5, 4, 2, 3 };

//#if MIXED_BUTTONS
const byte mixedButtonsInPin = A4;
//#else
//const byte buttonInPin[buttons] = { 53, 51, 49, 47, 45, 43 };
//#endif

const byte buttonOutPin[buttons] = { 52, 50, 48, 46, 44, 42 };

enum mode_t {
  MODE_LISTEN,
  MODE_PLAY
} mode = MODE_LISTEN;

bit_t modeLEDValue[2] = { OFF, OFF };

void setMode(int iNewMode)
{
  enum mode_t newMode = (mode_t)iNewMode;
  switch (newMode) {
    case MODE_LISTEN:
      modeLEDValue[0] = ON;
      modeLEDValue[1] = OFF;
      break;
    case MODE_PLAY:
      modeLEDValue[0] = OFF;
      modeLEDValue[1] = ON;
      break;
    default:
      modeLEDValue[0] = OFF;
      modeLEDValue[1] = OFF;
      break;
  }
  mode = newMode;
}

const int DEMUX_VAL_SIZE = 10;
const int BAR_UPPER_HALF_BOTTOM_INDEX = DEMUX_VAL_SIZE / 2;
const int BAR_LOWER_HALF_TOP_INDEX = BAR_UPPER_HALF_BOTTOM_INDEX - 1;

void setup() {
  Serial.begin(57600, SERIAL_8N1);

  analogReference(DEFAULT);

  for (int i = 0; i < ctrlLevers; i++) {
    pinMode(ctrlLeverInPin[i], INPUT);
    pinMode(ctrlLeverOutPin[i], OUTPUT);
    digitalWrite(ctrlLeverOutPin[i], OFF);
  }

//#if MIXED_BUTTONS
  pinMode(mixedButtonsInPin, INPUT);
//#else
//  for (int i = 0; i < buttons; i++) {
//    pinMode(buttonInPin[i], INPUT);
//  }
//#endif

  for (int i = 0; i < buttons; i++) {
    pinMode(buttonOutPin[i], OUTPUT);
    digitalWrite(buttonOutPin[i], RELEASED);
  }

  pinMode(demuxEnablePin, OUTPUT);
  digitalWrite(demuxEnablePin, LOW);
  pinMode(arduinoActivityLEDPin, OUTPUT);
  digitalWrite(arduinoActivityLEDPin, OFF);

  for (int demuxValBit = 0; demuxValBit < DEMUX_VAL_SIZE; demuxValBit++) {
    pinMode(demuxValBasePin + demuxValBit, OUTPUT);
    digitalWrite(demuxValBasePin + demuxValBit, LOW);
  }

  for (int demuxAdrBit = 0; demuxAdrBit < 3; demuxAdrBit++) {
    pinMode(demuxAdrBasePin + demuxAdrBit, OUTPUT);
    digitalWrite(demuxAdrBasePin + demuxAdrBit, LOW);
  }

  testLEDs(true);
  testLEDs(false);

  setMode(MODE_LISTEN);
}

void testLEDs(bool ledsOn)
{
  const byte LED_COUNT_MAX = 5 * DEMUX_VAL_SIZE;
  for (byte ledCount = 0; ledCount < LED_COUNT_MAX; ledCount++) {
    for (byte redisplay = 0; redisplay < 5; redisplay++) {
      for (byte demuxAdr = 0; demuxAdr < 5; demuxAdr++) {
        for (byte demuxAdrBit = 0; demuxAdrBit < 3; demuxAdrBit++) {
          const bit_t adrBit = bitRead(demuxAdr, demuxAdrBit);
          digitalWrite(demuxAdrBasePin + demuxAdrBit, adrBit ? HIGH : LOW);
        }
        for (byte demuxValBit = 0; demuxValBit < DEMUX_VAL_SIZE; demuxValBit++) {
          const bool valBit = ledsOn != (demuxAdr * DEMUX_VAL_SIZE + demuxValBit >= ledCount);
          digitalWrite(demuxValBasePin + demuxValBit, valBit ? HIGH : LOW);
        }
        digitalWrite(demuxEnablePin, HIGH);
        delay(4); // let the LEDs light for some time
        digitalWrite(demuxEnablePin, LOW);
      }
    }
  }
}

int ctrlLeverInRawValue[ctrlLevers] = { 0, 0, 0, 0 }; // 10 bit samples
byte ctrlLeverInValue[ctrlLevers] = { 0, 0, 0, 0 };
byte ctrlLeverOutValue[ctrlLevers] = { 0, 0, 0, 0 };
byte buttonInValue[buttons] = { 0, 0, 0, 0, 0, 0 }; // low-passed scaled value
bit_t buttonOutValue[buttons] = { RELEASED, RELEASED, RELEASED, RELEASED, RELEASED, RELEASED };

void setCtrlLeverInValue(const byte ctrlLeverIndex, const byte value, const bool simulation)
{
  if ((ctrlLeverIndex >= 0) && (ctrlLeverIndex < ctrlLevers)) {
    const byte simulationValue = ctrlLeverInValue[ctrlLeverIndex];
    ctrlLeverInValue[ctrlLeverIndex] = simulation ? simulationValue : value;
  } else {
    /* bad index => ignore */
  }
}

byte getCtrlLeverInValue(const byte ctrlLeverIndex)
{
  if ((ctrlLeverIndex >= 0) && (ctrlLeverIndex < ctrlLevers)) {
    return ctrlLeverInValue[ctrlLeverIndex];
  } else {
    return 0; /* bad index => assume 0 */
  }
}

void setCtrlLeverOutValue(const byte ctrlLeverIndex, const byte value)
{
  if ((ctrlLeverIndex >= 0) && (ctrlLeverIndex < ctrlLevers)) {
    ctrlLeverOutValue[ctrlLeverIndex] = value;
  } else {
    /* bad index => ignore */
  }
}

byte getCtrlLeverOutValue(const byte ctrlLeverIndex)
{
  if ((ctrlLeverIndex >= 0) && (ctrlLeverIndex < ctrlLevers)) {
    return ctrlLeverOutValue[ctrlLeverIndex];
  } else {
    return 0; /* bad index => assume 0 */
  }
}

bit_t getFullCtrlLeverInBarPixel(const byte ctrlLeverIndex, const byte pixelIndex)
{
  const int barValue = getCtrlLeverInValue(ctrlLeverIndex) * (DEMUX_VAL_SIZE + 1) / 256;
  return barValue > pixelIndex ? ON : OFF;
}

bit_t getCenteredBarPixel(const int barValue, const byte pixelIndex)
{
  return
    ((pixelIndex > BAR_LOWER_HALF_TOP_INDEX) && (barValue > pixelIndex)) ||
    ((pixelIndex < BAR_UPPER_HALF_BOTTOM_INDEX) && (barValue <= pixelIndex)) ? ON : OFF;
}

bit_t getCenteredCtrlLeverInBarPixel(const byte ctrlLeverIndex, const byte pixelIndex)
{
  const int barValue = getCtrlLeverInValue(ctrlLeverIndex) * (DEMUX_VAL_SIZE + 1) / 256;
  return getCenteredBarPixel(barValue, pixelIndex);
}

bit_t getReverseCenteredCtrlLeverInBarPixel(const byte ctrlLeverIndex, const byte pixelIndex)
{
  const int barValue = DEMUX_VAL_SIZE - getCtrlLeverInValue(ctrlLeverIndex) * (DEMUX_VAL_SIZE + 1) / 256;
  return getCenteredBarPixel(barValue, pixelIndex);
}

const static int hyst_min_value = 0;
const static int hyst_threshold = 5;
const static int hyst_max_value = 9;

bit_t getButtonInValue(const int buttonIndex)
{
  if ((buttonIndex >= 0) && (buttonIndex < buttons)) {
    //return buttonInValue[buttonIndex];
    return buttonInValue[buttonIndex] >= hyst_threshold ? PRESSED : RELEASED;
  } else {
    return RELEASED; /* bad index => assume RELEASED */
  }
}

void setButtonInValue(const int buttonIndex, const bit_t new_value, const bool simulation)
{
  if ((buttonIndex >= 0) && (buttonIndex < buttons)) {
    int value = buttonInValue[buttonIndex];
    if (new_value == HIGH) {
      if (value < hyst_max_value) {
        value++;
        if (value >= hyst_threshold) {
          value = hyst_max_value;
        }
      }
    } else /* (new_value == LOW) */ {
      if (value > hyst_min_value) {
        value--;
        if (value < hyst_threshold) {
          value = hyst_min_value;
        }
      }
    }
    const int simulationValue = buttonInValue[buttonIndex];
    buttonInValue[buttonIndex] = simulation ? simulationValue : value;
  } else {
    /* bad index => ignore */
  }
}

bit_t getButtonOutValue(const int buttonIndex)
{
  if ((buttonIndex >= 0) && (buttonIndex < buttons)) {
    return buttonOutValue[buttonIndex];
  } else {
    return RELEASED; /* bad index => assume RELEASED */
  }
}

void setButtonOutValue(const int buttonIndex, const bit_t value)
{
  if ((buttonIndex >= 0) && (buttonIndex < buttons)) {
    buttonOutValue[buttonIndex] = value;
  } else {
    /* bad index => ignore */
  }
}

const static byte columnIndex2ButtonInValue[10] = { 0, 1, -1, 3, 4, 5, 2, -1, -1, -1 };

bit_t getStatusLEDInValue(const int columnIndex)
{
  switch (columnIndex) {
    case 2:
    case 7:
      // currently unused
      return OFF;
    case 8:
    case 9:
      return modeLEDValue[columnIndex - 8];
    default:
      return getButtonInValue(columnIndex2ButtonInValue[columnIndex]);
  }
}

bit_t getLEDStatus(const int rowIndex, const int columnIndex)
{
  switch (rowIndex) {
    case 0: return getFullCtrlLeverInBarPixel(rowIndex, columnIndex);
    case 1: return getCenteredCtrlLeverInBarPixel(rowIndex, columnIndex);
    case 2: return getReverseCenteredCtrlLeverInBarPixel(rowIndex, columnIndex);
    case 3: return getReverseCenteredCtrlLeverInBarPixel(rowIndex, columnIndex);
    case 4: return getStatusLEDInValue(columnIndex);
    default: return OFF; /* bad row index => assume OFF */
  }
}

void updateQuadCopStatusDisplay()
{
  static int demuxAdr = 0;
  digitalWrite(demuxEnablePin, LOW);
  for (int demuxAdrBit = 0; demuxAdrBit < 3; demuxAdrBit++) {
    const bit_t adrBit = bitRead(demuxAdr, demuxAdrBit);
    digitalWrite(demuxAdrBasePin + demuxAdrBit, adrBit);
  }
  for (int demuxValBit = 0; demuxValBit < DEMUX_VAL_SIZE; demuxValBit++) {
    const bit_t ledStatus = getLEDStatus(demuxAdr, demuxValBit);
    digitalWrite(demuxValBasePin + demuxValBit, ledStatus);
  }
  digitalWrite(demuxEnablePin, HIGH);
  demuxAdr++;
  if (demuxAdr > 4) {
    demuxAdr = 0;
  }
}

void updateArduinoActivityLED()
{
  const long interval = 250;
  static long previousMillis = 0;
  static bit_t arduinoActivityLEDState = OFF;

  const unsigned long currentMillis = millis();
  if (currentMillis - previousMillis > interval) {
    previousMillis = currentMillis;
    if (arduinoActivityLEDState == OFF) {
      arduinoActivityLEDState = ON;
    } else {
      arduinoActivityLEDState = OFF;
    }
    digitalWrite(arduinoActivityLEDPin, arduinoActivityLEDState);
  }
}

void updateCtrlLeverInValues(const bool simulation)
{
  for (int i = 0; i < ctrlLevers; i++) {
    analogRead(ctrlLeverInPin[i]); // discard possibly inprecise 1st try
    const int newCtrlLeverInRawValue = analogRead(ctrlLeverInPin[i]);

    // reduce jitter via gate function
    const int diff = newCtrlLeverInRawValue - ctrlLeverInRawValue[i];
    if ((diff < -3) || (diff > +3)) {
      ctrlLeverInRawValue[i] = newCtrlLeverInRawValue;
      setCtrlLeverInValue(i, newCtrlLeverInRawValue >> 2, simulation);
    }
  }
}

void emitCtrlLeverOutValues()
{
  for (int i = 0; i < ctrlLevers; i++) {
    analogWrite(ctrlLeverOutPin[i], getCtrlLeverOutValue(i));
  }
}

void updateButtonInValues(const bool simulation)
{

//#if MIXED_BUTTONS
  analogRead(mixedButtonsInPin); // discard possibly inprecise 1st try
  const int value = analogRead(mixedButtonsInPin);
  setButtonInValue(0, value < 40 ? PRESSED : RELEASED, simulation);
  setButtonInValue(2, (value >= 40) && (value < 120) ? PRESSED : RELEASED, simulation);
  setButtonInValue(3, (value >= 120) && (value < 200) ? PRESSED : RELEASED, simulation);
  setButtonInValue(1, (value >= 200) && (value < 300) ? PRESSED : RELEASED, simulation);
  setButtonInValue(4, (value >= 300) && (value < 385) ? PRESSED : RELEASED, simulation);
  setButtonInValue(5, (value >= 385) && (value < 570) ? PRESSED : RELEASED, simulation);
//#else
//  for (int i = 0; i < buttons; i++) {
//    setButtonInValue(i, digitalRead(buttonInPin[i]), simulation);
//  }
//#endif


#if 0
  static int count = 0;
  count = (count + 1) & 0x3f;
  if (count == 0) {
    Serial.print("[val=");
    Serial.print(value, DEC);
    Serial.println("]");
  }
#endif
}

void emitButtonOutValues()
{
  for (int i = 0; i < buttons; i++) {
    digitalWrite(buttonOutPin[i], getButtonOutValue(i));
  }
}

// simple low pass filter configuration
const double alpha = 0.5;
const double beta = 1.0 - alpha;

void loopbackIn2Out()
{
  for (int i = 0; i < ctrlLevers; i++) {
    const double newOutValue = alpha * getCtrlLeverInValue(i) + beta * getCtrlLeverOutValue(i);
    setCtrlLeverOutValue(i, (int)newOutValue);
  }

  // buttons need not being looped back, since
  // their original wiring has not been cut
}

void loopbackOut2In()
{
  for (int i = 0; i < ctrlLevers; i++) {
    setCtrlLeverInValue(i, getCtrlLeverOutValue(i), false);
  }

  // buttons need not being looped back, since
  // their original wiring has not been cut
}

/*
const int RX_BUFFER_SIZE = 512;
int rxWritePtr = 0;
int rxReadPtr = 0;

byte inBuffer[RX_BUFFER_SIZE];

int getRXAvailable()
{
  const int available = RX_BUFFER_SIZE + rxWritePtr - rxReadPtr;
  if (available >= RX_BUFFER_SIZE) {
    available -= RX_BUFFER_SIZE;
  }
  return available;
}
*/

byte getRXBufferFillLevel()
{
  //return (64 * getRXAvailable()) / RX_BUFFER_SIZE;
  return Serial.available();
}

void reportStatus()
{
  Serial.write(0x80 | getRXBufferFillLevel()); // status byte
  byte wrappedBits = 0x0;
  for (int i = 0; i < ctrlLevers; i++) {
    const byte ctrlLeverInValue = getCtrlLeverInValue(i);
    const byte serialValue = wrappedBits | (ctrlLeverInValue >> (i + 1));
    Serial.write(serialValue);
    wrappedBits = (ctrlLeverInValue << (6 - i)) & 0x7f;
  }
  Serial.write(wrappedBits);
  byte allButtonsValue = 0;
  for (int i = 0; i < buttons; i++) {
    allButtonsValue = allButtonsValue << 1 | getButtonInValue(i);
  }
  Serial.write(allButtonsValue);
}

enum rxStatus_t {
  RX_STATUS_UNSYNCHRONIZED,
  RX_STATUS_STATUS_READ,
  RX_STATUS_CTRL_LEVER_BYTE0_READ,
  RX_STATUS_CTRL_LEVER_BYTE1_READ,
  RX_STATUS_CTRL_LEVER_BYTE2_READ,
  RX_STATUS_CTRL_LEVER_BYTE3_READ,
  RX_STATUS_CTRL_LEVER_BYTE4_READ,
  RX_STATUS_BUTTONS_READ
};

rxStatus_t rxStatus = RX_STATUS_UNSYNCHRONIZED;

byte rx_status;
byte rx_ctrlLever0;
byte rx_ctrlLever1;
byte rx_ctrlLever2;
byte rx_ctrlLever3;
byte rx_buttons;

void recordReceived()
{
  setMode(MODE_PLAY);
  setCtrlLeverOutValue(0, rx_ctrlLever0);
  setCtrlLeverOutValue(1, rx_ctrlLever1);
  setCtrlLeverOutValue(2, rx_ctrlLever2);
  setCtrlLeverOutValue(3, rx_ctrlLever3);
  /*
  byte allButtonsValue = rx_buttons;
  for (int i = 0; i < buttons; i++) {
    setButtonOutValue(buttons - i, allButtonsValue & 0x1);
    allButtonsValue >>= 1;
  }
  */
}

void handleByte(const byte b) {
  switch (rxStatus) {
  case RX_STATUS_UNSYNCHRONIZED:
  case RX_STATUS_BUTTONS_READ:
    if (b >= 0x80) {
      rx_status = b;
      rxStatus = RX_STATUS_STATUS_READ;
    } else {
      // still or newly lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    break;
  case RX_STATUS_STATUS_READ:
    if (b < 0x80) {
      rx_ctrlLever0 = b << 1;
      rxStatus = RX_STATUS_CTRL_LEVER_BYTE0_READ;
    } else {
      // lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    break;
  case RX_STATUS_CTRL_LEVER_BYTE0_READ:
    if (b < 0x80) {
      rx_ctrlLever0 |= b >> 6;
      rx_ctrlLever1 = b << 2;
      rxStatus = RX_STATUS_CTRL_LEVER_BYTE1_READ;
    } else {
      // lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    break;
  case RX_STATUS_CTRL_LEVER_BYTE1_READ:
    if (b < 0x80) {
      rx_ctrlLever1 |= b >> 5;
      rx_ctrlLever2 = b << 3;
      rxStatus = RX_STATUS_CTRL_LEVER_BYTE2_READ;
    } else {
      // lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    break;
  case RX_STATUS_CTRL_LEVER_BYTE2_READ:
    if (b < 0x80) {
      rx_ctrlLever2 |= b >> 4;
      rx_ctrlLever3 = b << 4;
      rxStatus = RX_STATUS_CTRL_LEVER_BYTE3_READ;
    } else {
      // lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    break;
  case RX_STATUS_CTRL_LEVER_BYTE3_READ:
    if (b < 0x80) {
      rx_ctrlLever3 |= b >> 3;
      rxStatus = RX_STATUS_CTRL_LEVER_BYTE4_READ;
    } else {
      // lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    break;
  case RX_STATUS_CTRL_LEVER_BYTE4_READ:
    if (b < 0x80) {
      rx_buttons = b;
      rxStatus = RX_STATUS_BUTTONS_READ;
    } else {
      // lost synchronisation
      rxStatus = RX_STATUS_UNSYNCHRONIZED;
    }
    recordReceived();
    break;
  default:
    rxStatus = RX_STATUS_UNSYNCHRONIZED;
    break;
  }
}

void receiveStatus()
{
  if (Serial.available() > 6) {
    for (int i = 0; i < 6; i++) {
      int serialValue = Serial.read();
      handleByte((byte)serialValue);
    }
  } else {
    // buffer underrun
    setMode(MODE_LISTEN);
  }
}

void loop()
{
  updateArduinoActivityLED();
  updateQuadCopStatusDisplay();
  switch (mode) {
    case MODE_LISTEN:
      updateCtrlLeverInValues(false);
      updateButtonInValues(false);
      receiveStatus();
      loopbackIn2Out();
      break;
    case MODE_PLAY:
      updateCtrlLeverInValues(true);
      updateButtonInValues(true);
      receiveStatus();
      loopbackOut2In();
      break;
    default:
      break;
  }
  reportStatus();
  emitCtrlLeverOutValues();
  //emitButtonOutValues();
}


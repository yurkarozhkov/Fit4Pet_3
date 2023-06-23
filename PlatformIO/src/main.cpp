#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Arduino.h>
#include <Wire.h>

#define pin11 23
#define pin12 22
#define pin13 21
#define pin14 19
#define pin15 18
#define pin16 5
#define pin17 17
#define pin18 16
#define pin21 15
#define pin22 2
#define pin23 4
#define pin24 13
#define pin25 12
#define pin26 14
#define pin27 27

#define pinK1 36
#define pinK2 39
#define pinKn5 34
#define pinKn6 35

#define pinDAC 25
#define pinPWM 26
#define pinRelayOn 33

// setting PWM properties
const int freq = 5000;
const int ledChannel = 0;
const int resolution = 10; //Resolution 8, 10, 12, 15

int pinRelay[15] = {pin11,
                    pin12,
                    pin13,
                    pin14,
                    pin15,
                    pin16,
                    pin17,
                    pin18,
                    pin21,
                    pin22,
                    pin23,
                    pin24,
                    pin25,
                    pin26,
                    pin27};

uint16_t Data[5] = {
    0b1111111111111001, //Кн1 - Залив в дорожку из бака
    0b1111111111110110, //Кн2 - Слив из дорожки в бак
    0b1111101011101110, //Кн3 - Противоток
    0b1111111111011110, //Кн4 - Из дорожки в канализацию
    0b1110111111011011  //Кн5 - Из бака в канализацию
};

void writeDAC(uint8_t dac1)
{
  dacWrite(pinDAC, map(dac1, 0, 100, 0, 255));
}

void writePWM(uint16_t PWM1)
{
  ledcWrite(ledChannel, PWM1);
}

BLEServer *pServer = NULL;
BLECharacteristic *pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint8_t value = 0;
uint8_t value1 = 0;
boolean K1 = 0;
boolean K2 = 0;
boolean Kn5 = 0;
boolean Kn6 = 0;

boolean firstStart = true;
uint_fast32_t timeConnection = 0;

//Параметр для организации антидребезга
const int countDrebezg = 250;

uint8_t k1Count = 0;
uint8_t k2Count = 0;
uint8_t kn5Count = 0;
uint8_t kn6Count = 0;

bool charging = true;
// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
//#define CHARACTERISTIC1_UUID "d0df213a-15ff-4666-8408-17f553dc7a14"
//#define CHARACTERISTIC2_UUID "b344dcdc-2490-42f4-a995-d4bf858333c8"

class MyServerCallbacks : public BLEServerCallbacks
{
  void onConnect(BLEServer *pServer)
  {

    Serial.println("Device Connected: ");

    timeConnection = millis();
    Serial.println(timeConnection);
    deviceConnected = true;
  };

  void onDisconnect(BLEServer *pServer)
  {
    Serial.println("Device DisConnected: ");
    firstStart = true;
    deviceConnected = false;
  }
};

class MyCallbacks : public BLECharacteristicCallbacks
{

  uint8_t byte1 = 0;
  uint8_t byte2 = 0;
  uint8_t byte3 = 0;
  uint8_t byte4 = 0;
  uint8_t speed10 = 0;
  uint8_t tok = 0;
  uint16_t rezhim = 0;

  void printSerialBin(uint16_t val)
  {
    for (int i = 0; i <= 15; i++)
    {
      Serial.print(((val >> i) & 1) ? 1 : 0);
      if (i == 3 or i == 7 or i == 11)
        Serial.print(" ");
    }
  }
  //--------------------------------------------------------------------------------------------------
  word setPinOn(word num, byte pos)
  { //Устанавливаем нужный бит в 1
    return (num | (1 << (pos)));
  }

  //--------------------------------------------------------------------------------------------------
  word setPinOff(word num, byte pos)
  { //Устанавливаем нужный бит в 0
    return (num & ~(1 << (pos)));
  }

  //--------------------------------------------------------------------------------------------------
  word setPinSwitch(word num, byte pos)
  { //Инвертируем нужный бит
    return (num ^ (1 << (pos)));
  }

  //--------------------------------------------------------------------------------------------------
  uint8_t checkbit(const uint16_t value, const uint8_t position)
  {
    if ((value & (1 << position)) == 0)
      return 0;
    else
      return 1;
  }

  void setAllPins(uint16_t num)
  { //Устанавливаем выходы в соответствии с режимом
    for (int i = 0; i < 15; i++)
    {
      digitalWrite(pinRelay[i], checkbit(num, i));
    }
  }

  //Процедура обработки полученных данных о текущем режиме
  void setOutput(byte b1, byte b2)
  {
    uint16_t Rezhim = 0xFFFF;
    if ((b1 & 1) != 0)
      Rezhim = Data[0]; //Бит 1:0 - включен режим 1 - Залив в дорожку из бака
    if ((b1 & (1 << 1)) != 0)
      Rezhim = Data[1]; //Бит 1:1 - включен режим 2 - Слив из дорожки в бак
    if ((b1 & (1 << 2)) != 0)
      Rezhim = Data[2]; //Бит 1:2 - включен режим 3 - Противоток
    if ((b1 & (1 << 3)) != 0)
      Rezhim = Data[3]; //Бит 1:3 - включен режим 4 - Слив из дорожки в канализацию
    if ((b1 & (1 << 4)) != 0)
    { //Бит 1:4 - включен режим 5 - Слив из бака в канализацию
      Rezhim = Data[4];
      Kn5 = 1;
    }
    else
      Kn5 = 0;
    if ((b1 & (1 << 5)) != 0)
    { //Бит 1:5 - включен режим 6 - Наполнение бака
      Rezhim = setPinOff(Rezhim, 6);
      Rezhim = setPinOff(Rezhim, 13);
      Kn6 = 1;
    }
    else
      Kn6 = 0;
    if ((b1 & (1 << 6)) != 0)
    { //Бит 1:6 - включение мотора
      Rezhim = setPinOff(Rezhim, 7);
    }
    if ((b1 & (1 << 7)) != 0)
    { //Бит 1:7 - Реверс
      Rezhim = setPinOff(Rezhim, 11);
    }
    if ((b2 & 1) != 0)
    { //Бит 2:0 - Включение помпы
      Rezhim = setPinOff(Rezhim, 8);
      Rezhim = setPinOff(Rezhim, 9);
    }
    if ((b2 & (1 << 1)) != 0)
    { //Бит 2:1 - Включение Кран8 (резерв)
      Rezhim = setPinOff(Rezhim, 15);
    }
    //if ((b2 & (1 << 2)) != 0)
    if (charging)
      //Бит 2:1 - Включение Зарядки АКБ
      Rezhim = setPinOff(Rezhim, 14);
    else
      Rezhim = setPinOn(Rezhim, 14);

    setAllPins(Rezhim); //Применить изменения
  }

  void onWrite(BLECharacteristic *pCharacteristic)
  {
    std::string rxValue = pCharacteristic->getValue();
    if (rxValue.length() == 4)
    {
      byte1 = (uint8_t)rxValue[0];
      byte2 = (uint8_t)rxValue[1];
      byte3 = (uint8_t)rxValue[2];
      byte4 = (uint8_t)rxValue[3];
      rezhim = word(byte1, byte2);
      speed10 = byte4;
      tok = byte3;
      setOutput(byte1, byte2);
      writeDAC(tok);
      writePWM(map(speed10, 0, 160, 0, 1024));
    }
    else
      Serial.println("Wrong format recieved data!!!");
  }
};

void setup()
{
  pinMode(pinRelayOn, OUTPUT);
  digitalWrite(pinRelayOn, LOW);
  delay(1000);

  for (int i = 0; i <= 14; i++)
  {
    pinMode(pinRelay[i], OUTPUT);
    digitalWrite(pinRelay[i], HIGH);
  }
  pinMode(pinK1, INPUT);
  pinMode(pinK2, INPUT);
  pinMode(pinKn5, INPUT);
  pinMode(pinKn6, INPUT);

  pinMode(pinDAC, OUTPUT);
  pinMode(pinPWM, OUTPUT);
  // configure LED PWM functionalitites
  ledcSetup(ledChannel, freq, resolution);

  // attach the channel to the GPIO2 to be controlled
  ledcAttachPin(pinPWM, ledChannel);

  Serial.begin(115200);

  // Create the BLE Device
  BLEDevice::init("MyESP32");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY);

  pCharacteristic->setCallbacks(new MyCallbacks());

  // https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
  // Create a BLE Descriptor
  pCharacteristic->addDescriptor(new BLE2902());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println();
  Serial.println("Waiting a client connection to notify...");
  digitalWrite(pinRelayOn, HIGH);
}

void getInput()
{
  if (K1 != digitalRead(pinK1))
  { //Проверка изменившегося значения концевика К1
    k1Count++;
    if (k1Count >= countDrebezg)
    {
      K1 = !K1;
      Serial.print("Koncevik1=");
      Serial.println(K1);
      k1Count = 0;
    }
  }
  else
    k1Count = 0;

  if (K2 != digitalRead(pinK2))
  { //Проверка изменившегося значения концевика К2
    k2Count++;
    if (k2Count >= countDrebezg)
    {
      K2 = !K2;
      Serial.print("Koncevik2=");
      Serial.println(K2);
      k2Count = 0;
    }
  }
  else
    k2Count = 0;

  if (Kn5 != digitalRead(pinKn5))
  { //Проверка нажатия кнопки Kn5
    kn5Count++;
    if (kn5Count >= countDrebezg)
    {
      Kn5 = !Kn5;
      if (!Kn5)
        Serial.print("Kn5");
      kn5Count = 0;
    }
  }
  else
    kn5Count = 0;

  if (Kn6 != digitalRead(pinKn6))
  { //Проверка нажатия кнопки Kn6
    kn6Count++;
    if (kn6Count >= countDrebezg)
    {
      Kn6 = !Kn6;
      if (!Kn6)
        Serial.print("Kn6");
      kn6Count = 0;
    }
  }
  else
    kn6Count = 0;
}

void loop()
{

  getInput();

  // notify changed value
  if (deviceConnected)
  {
    if (firstStart && ((millis() - timeConnection) > 5000))
    {
      Serial.println("1 notify" + millis());
      firstStart = false;
      value = (K1 << 3) + (K2 << 2) + (Kn5 << 1) + Kn6;
      pCharacteristic->setValue(&value, 1);
      pCharacteristic->notify();
    }
    value1 = (K1 << 3) + (K2 << 2) + (Kn5 << 1) + Kn6;

    if (value1 != value)
    {
      value = value1;
      pCharacteristic->setValue(&value, 1);
      pCharacteristic->notify();
      Serial.println("value=" + value);
    }
  }
}

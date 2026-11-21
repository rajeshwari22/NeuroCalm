import 'package:flutter/foundation.dart';
import '../models/device_model.dart';

/// BLE layer for the ESP32. CURRENTLY A DEMO STUB: no real Bluetooth is used.
/// Sample devices are flagged `isSample` and no commands reach any hardware.
/// To integrate real BLE, add `flutter_blue_plus`, add the Android permissions
/// listed in README.md, and implement the TODO blocks below.
class BluetoothService extends ChangeNotifier {
  BluetoothService._();
  static final instance = BluetoothService._();

  List<NeuroDevice> devices = [];
  NeuroDevice? connected;
  bool scanning = false;

  bool get isConnected => connected != null;
  bool get isDemoConnection => connected?.isSample ?? false;

  Future<void> scan() async {
    scanning = true;
    notifyListeners();
    // TODO: FlutterBluePlus.startScan(...) and map results to NeuroDevice.
    await Future.delayed(const Duration(seconds: 1));
    devices = const [
      NeuroDevice(id: 'sample-1', name: 'NeuroCalm-01', rssi: -50, isSample: true),
      NeuroDevice(id: 'sample-2', name: 'NeuroCalm-02', rssi: -62, isSample: true),
      NeuroDevice(id: 'sample-3', name: 'NeuroCalm-03', rssi: -78, isSample: true),
    ];
    scanning = false;
    notifyListeners();
  }

  Future<void> connect(NeuroDevice d) async {
    // TODO: device.connect(), discover GATT services/characteristics.
    await Future.delayed(const Duration(milliseconds: 500));
    connected = d;
    notifyListeners();
  }

  Future<void> disconnect() async {
    // TODO: device.disconnect() and cancel notification subscriptions.
    connected = null;
    notifyListeners();
  }

  /// Sends a command string (e.g. "START;L;SEQ;BOTH;300") to the ESP32.
  /// Returns false when nothing was actually transmitted (always the case now).
  Future<bool> sendCommand(String command) async {
    // TODO: write to the ESP32 command characteristic.
    return false;
  }

  /// TODO: subscribe to the ESP32 sensor characteristic and emit parsed data.
  Stream<List<double>> get sensorStream => const Stream.empty();
}

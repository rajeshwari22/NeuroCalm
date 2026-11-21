/// A BLE device. [isSample] is true for demo entries that are NOT real scan results.
class NeuroDevice {
  final String id;
  final String name;
  final int rssi; // dBm
  final bool isSample;
  const NeuroDevice({required this.id, required this.name, required this.rssi, this.isSample = false});

  /// 1..4 signal bars derived from RSSI.
  int get bars => rssi > -55 ? 4 : rssi > -65 ? 3 : rssi > -75 ? 2 : 1;
}

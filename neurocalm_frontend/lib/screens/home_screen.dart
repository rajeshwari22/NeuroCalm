import 'dart:async';
import 'package:flutter/material.dart';
import '../models/session_model.dart';
import '../services/backend_service.dart';
import '../services/bluetooth_service.dart';
import '../services/massage_service.dart';
import '../utils/app_theme.dart';
import '../widgets/action_buttons.dart';
import '../widgets/bottom_sheet.dart';
import '../widgets/monitoring_graph.dart';
import '../widgets/neuro_logo.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _bt = BluetoothService.instance;
  final _backend = BackendService.instance;
  late SensorSnapshot _data = _backend.sample();
  Timer? _poll;

  @override
  void initState() {
    super.initState();
    _bt.addListener(_onBt);
  }

  void _onBt() {
    _poll?.cancel();
    if (_bt.isConnected) {
      _refresh();
      _poll = Timer.periodic(const Duration(seconds: 3), (_) => _refresh());
    } else if (mounted) {
      setState(() => _data = _backend.sample());
    }
    if (mounted) setState(() {});
  }

  Future<void> _refresh() async {
    // Live data only when connected; falls back to sample if backend unavailable.
    final live = await _backend.fetchLive();
    if (mounted) setState(() => _data = live ?? _backend.sample());
  }

  @override
  void dispose() {
    _poll?.cancel();
    _bt.removeListener(_onBt);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final connected = _bt.isConnected;
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(colors: [AppColors.lavender, AppColors.bg], begin: Alignment.topCenter, end: Alignment.bottomCenter),
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(children: [
              Row(children: [
                const NeuroLogo(size: 40),
                const SizedBox(width: 10),
                const Text('NeuroCalm', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.primary)),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20)),
                  child: Row(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.circle, size: 10, color: connected ? AppColors.ok : Colors.grey),
                    const SizedBox(width: 6),
                    Text(connected ? (_bt.isDemoConnection ? 'Connected (demo)' : 'Connected') : 'Disconnected', style: const TextStyle(fontSize: 12)),
                  ]),
                ),
              ]),
              const SizedBox(height: 18),
              const Text('Welcome to NeuroCalm', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800, color: AppColors.deep)),
              const Text('Relax • Monitor • Feel Better', style: TextStyle(color: AppColors.deep)),
              const SizedBox(height: 18),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 1.5,
                children: [
                  ActionCard(icon: Icons.bluetooth, label: 'Connection', color: AppColors.lightBlue, onTap: () => showConnectionSheet(context)),
                  ActionCard(icon: Icons.spa, label: 'Massage', color: AppColors.lavender, onTap: () => showMassageSheet(context, latest: _data)),
                  ActionCard(icon: Icons.history, label: 'History', color: AppColors.lavender, onTap: () => showHistorySheet(context, _replay)),
                  ActionCard(icon: Icons.settings, label: 'Settings', color: AppColors.lightBlue, onTap: () => showSettingsSheet(context)),
                ],
              ),
              const SizedBox(height: 18),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Colors.white.withValues(alpha: 0.7), borderRadius: BorderRadius.circular(18)),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  const Row(children: [
                    Icon(Icons.monitor_heart, color: AppColors.primary),
                    SizedBox(width: 8),
                    Text('Monitoring', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16)),
                  ]),
                  const SizedBox(height: 4),
                  Text(
                    '${_data.isLive ? 'Live backend data' : 'Sample data'}'
                    '${_data.heartRate != null ? '  •  HR ${_data.heartRate} bpm' : ''}',
                    style: TextStyle(fontSize: 12, color: _data.isLive ? AppColors.ok : Colors.black54),
                  ),
                  const SizedBox(height: 8),
                  Row(children: [
                    Expanded(child: MonitoringGraph(title: 'Session Graph', values: _data.ppg, color: AppColors.primary)),
                    const SizedBox(width: 10),
                    Expanded(child: MonitoringGraph(title: 'Blood Volume', values: _data.bvp, color: const Color(0xFF3B8EEA))),
                  ]),
                ]),
              ),
            ]),
          ),
        ),
      ),
    );
  }

  void _replay(s) {
    // Loads settings only. Never starts motors automatically.
    MassageService.instance.applySession(s);
    showMassageSheet(context, latest: _data);
  }
}

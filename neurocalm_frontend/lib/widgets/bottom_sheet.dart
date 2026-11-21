import 'package:flutter/material.dart';
import '../models/device_model.dart';
import '../models/massage_model.dart';
import '../models/session_model.dart';
import '../screens/login_screen.dart';
import '../services/auth_service.dart';
import '../services/backend_service.dart';
import '../services/bluetooth_service.dart';
import '../services/massage_service.dart';
import '../utils/app_theme.dart';

Future<void> _showSheet(BuildContext context, Widget child, {bool dark = false}) =>
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (_) => Container(
        decoration: BoxDecoration(
          color: dark ? null : Colors.white,
          gradient: dark
              ? const LinearGradient(colors: [Color(0xFF6A5AE0), Color(0xFF4B3FC9)], begin: Alignment.topLeft, end: Alignment.bottomRight)
              : null,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(26)),
        ),
        padding: EdgeInsets.fromLTRB(18, 16, 18, 18 + MediaQuery.of(context).viewInsets.bottom),
        child: SingleChildScrollView(child: child),
      ),
    );

class _Header extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool light;
  const _Header(this.icon, this.title, {this.light = false});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: Row(children: [
          Icon(icon, color: light ? Colors.white : AppColors.primary),
          const SizedBox(width: 10),
          Expanded(child: Text(title, style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: light ? Colors.white : AppColors.deep))),
          IconButton(icon: Icon(Icons.close, color: light ? Colors.white : null), onPressed: () => Navigator.pop(context)),
        ]),
      );
}

// ---------------------------------------------------------------- Connection
void showConnectionSheet(BuildContext context) {
  final bt = BluetoothService.instance;
  if (bt.devices.isEmpty && !bt.scanning) bt.scan();
  _showSheet(
    context,
    ListenableBuilder(
      listenable: bt,
      builder: (context, _) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const _Header(Icons.bluetooth, 'Connect to Device'),
        Text(
          bt.isConnected ? 'Connected: ${bt.connected!.name}${bt.isDemoConnection ? ' (demo - no hardware)' : ''}' : 'Status: Not connected',
          style: TextStyle(color: bt.isConnected ? AppColors.ok : Colors.black54, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 4),
        const Text('Sample devices shown below are demo entries, not real Bluetooth scan results.',
            style: TextStyle(fontSize: 12, color: Colors.black54)),
        const SizedBox(height: 12),
        const Text('Devices in Range:', style: TextStyle(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        if (bt.scanning) const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
        if (!bt.scanning)
          for (final d in bt.devices) _DeviceTile(d, bt),
        const SizedBox(height: 8),
        Row(children: [
          Expanded(
            child: OutlinedButton.icon(
              onPressed: bt.scanning ? null : bt.scan,
              icon: const Icon(Icons.refresh),
              label: const Text('Scan for Devices'),
            ),
          ),
          if (bt.isConnected) ...[
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                style: OutlinedButton.styleFrom(foregroundColor: AppColors.danger),
                onPressed: () async {
                  await MassageService.instance.stop(reason: 'Device disconnected - session stopped.');
                  await bt.disconnect();
                },
                icon: const Icon(Icons.link_off),
                label: const Text('Disconnect'),
              ),
            ),
          ],
        ]),
      ]),
    ),
  );
}

class _DeviceTile extends StatelessWidget {
  final NeuroDevice d;
  final BluetoothService bt;
  const _DeviceTile(this.d, this.bt);
  @override
  Widget build(BuildContext context) {
    final isThis = bt.connected?.id == d.id;
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(color: AppColors.bg, borderRadius: BorderRadius.circular(14)),
      child: Row(children: [
        const Icon(Icons.headset, color: AppColors.deep),
        const SizedBox(width: 10),
        Expanded(child: Text(d.name, style: const TextStyle(fontWeight: FontWeight.w600))),
        Row(children: [
          for (var i = 1; i <= 4; i++)
            Container(
              width: 4,
              height: 5.0 + i * 4,
              margin: const EdgeInsets.only(left: 2),
              color: i <= d.bars ? AppColors.ok : Colors.grey.shade300,
            ),
        ]),
        const SizedBox(width: 10),
        FilledButton(
          onPressed: isThis ? null : () => bt.connect(d),
          style: FilledButton.styleFrom(minimumSize: const Size(84, 38)),
          child: Text(isThis ? 'Connected' : 'Connect'),
        ),
      ]),
    );
  }
}

// ------------------------------------------------------------------- Massage
void showMassageSheet(BuildContext context, {SensorSnapshot? latest}) {
  final m = MassageService.instance;
  _showSheet(
    context,
    ListenableBuilder(
      listenable: m,
      builder: (context, _) => m.active ? _ProgressView(m) : _SetupView(m, latest),
    ),
  );
}

class _SetupView extends StatefulWidget {
  final MassageService m;
  final SensorSnapshot? latest;
  const _SetupView(this.m, this.latest);
  @override
  State<_SetupView> createState() => _SetupViewState();
}

class _SetupViewState extends State<_SetupView> {
  bool recHidden = false;

  @override
  Widget build(BuildContext context) {
    final m = widget.m;
    final durations = {1, 2, 5, 10, 15, 20, MassageService.maxMinutes, m.minutes}.toList()..sort();
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const _Header(Icons.spa, 'Massage Modes'),
      Row(children: [
        for (final i in Intensity.values)
          Expanded(child: IntensityCard(intensity: i, selected: m.intensity == i, onTap: () => m.setIntensity(i))),
      ]),
      const SizedBox(height: 4),
      Text(m.intensity.description, style: const TextStyle(fontSize: 12, color: Colors.black54)),
      const SizedBox(height: 12),
      const Text('Massage Pattern', style: TextStyle(fontWeight: FontWeight.w700)),
      const SizedBox(height: 6),
      _drop<MassagePattern>(m.pattern, MassagePattern.values, (p) => p.label, m.setPattern),
      const SizedBox(height: 12),
      Row(children: [
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Motor Zone', style: TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 6),
          _drop<MotorZone>(m.zone, MotorZone.values, (z) => z.label, m.setZone),
        ])),
        const SizedBox(width: 10),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('Duration', style: TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 6),
          _drop<int>(m.minutes, durations, (d) => '$d min', m.setMinutes),
        ])),
      ]),
      const SizedBox(height: 4),
      const Text('Temple Zone = 2 motors together. Rear Zone = 1 independent motor.',
          style: TextStyle(fontSize: 11, color: Colors.black54)),
      const SizedBox(height: 14),
      if (m.message != null)
        Padding(padding: const EdgeInsets.only(bottom: 8), child: Text(m.message!, style: const TextStyle(color: AppColors.danger))),
      SizedBox(
        width: double.infinity,
        child: FilledButton.icon(onPressed: m.start, icon: const Icon(Icons.play_arrow), label: const Text('Start Massage')),
      ),
      const SizedBox(height: 14),
      if (!recHidden) _recommendation(m),
    ]);
  }

  Widget _drop<T>(T value, List<T> items, String Function(T) label, void Function(T) onChanged) =>
      DropdownButtonFormField<T>(
        initialValue: value,
        isExpanded: true,
        decoration: InputDecoration(
          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          filled: true,
          fillColor: AppColors.bg,
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
        ),
        items: [for (final i in items) DropdownMenuItem(value: i, child: Text(label(i), overflow: TextOverflow.ellipsis))],
        onChanged: (v) => v == null ? null : onChanged(v),
      );

  /// Recommendation only from LIVE, good-quality data. Never auto-starts.
  /// The rule below is a PLACEHOLDER heuristic, not clinical guidance.
  Widget _recommendation(MassageService m) {
    final s = widget.latest;
    final reliable = s != null && s.isLive && s.quality == 'good' && s.heartRate != null;
    final rec = reliable ? (s.heartRate! >= 80 ? Intensity.light : Intensity.medium) : null;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: AppColors.lavender.withValues(alpha: 0.6), borderRadius: BorderRadius.circular(16)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const Row(children: [
          Icon(Icons.auto_awesome, color: AppColors.primary),
          SizedBox(width: 8),
          Text('System Recommendation', style: TextStyle(fontWeight: FontWeight.w700)),
        ]),
        const SizedBox(height: 8),
        if (rec == null)
          const Text('Sensor reading: Unable to assess')
        else ...[
          Text('Based on your recent physiological readings, we recommend a ${rec.label} Massage.'),
          const SizedBox(height: 10),
          Wrap(spacing: 8, runSpacing: 8, children: [
            FilledButton(
              style: FilledButton.styleFrom(minimumSize: const Size(80, 40)),
              onPressed: () { m.setIntensity(rec); setState(() => recHidden = true); },
              child: const Text('Accept'),
            ),
            OutlinedButton(onPressed: () => setState(() => recHidden = true), child: const Text('Reject')),
            OutlinedButton(onPressed: () => setState(() => recHidden = true), child: const Text('Choose Another Mode')),
          ]),
          const SizedBox(height: 4),
          const Text('Accepting only selects the mode; you still press Start.', style: TextStyle(fontSize: 11, color: Colors.black54)),
        ],
      ]),
    );
  }
}

class IntensityCard extends StatelessWidget {
  final Intensity intensity;
  final bool selected;
  final VoidCallback onTap;
  const IntensityCard({super.key, required this.intensity, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final icon = switch (intensity) {
      Intensity.light => Icons.eco,
      Intensity.medium => Icons.spa,
      Intensity.heavy => Icons.fitness_center,
    };
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.symmetric(horizontal: 4),
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: selected ? AppColors.lavender : AppColors.bg,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: selected ? AppColors.primary : Colors.transparent, width: 2),
        ),
        child: Column(children: [
          Icon(icon, color: AppColors.primary, size: 28),
          const SizedBox(height: 4),
          Text(intensity.label, style: const TextStyle(fontWeight: FontWeight.w600)),
          Text('(${intensity.defaultMinutes} min)', style: const TextStyle(fontSize: 12)),
        ]),
      ),
    );
  }
}

class _ProgressView extends StatelessWidget {
  final MassageService m;
  const _ProgressView(this.m);
  @override
  Widget build(BuildContext context) {
    final mm = (m.remaining ~/ 60).toString().padLeft(2, '0');
    final ss = (m.remaining % 60).toString().padLeft(2, '0');
    const w = TextStyle(color: Colors.white);
    return Column(children: [
      const _Header(Icons.spa, 'Massage in Progress', light: true),
      SizedBox(
        width: 180,
        height: 180,
        child: Stack(alignment: Alignment.center, children: [
          SizedBox.expand(
            child: CircularProgressIndicator(
              value: m.progress,
              strokeWidth: 8,
              color: Colors.white,
              backgroundColor: Colors.white24,
            ),
          ),
          Column(mainAxisSize: MainAxisSize.min, children: [
            Text('$mm:$ss', style: const TextStyle(color: Colors.white, fontSize: 40, fontWeight: FontWeight.w700)),
            Text(m.state == RunState.paused ? 'Paused' : 'Remaining', style: w),
          ]),
        ]),
      ),
      const SizedBox(height: 14),
      Text('Intensity: ${m.intensity.label}', style: w),
      Text('Pattern: ${m.pattern.label}', style: w),
      Text('Zone: ${m.zone.label}', style: w),
      const SizedBox(height: 16),
      Wrap(alignment: WrapAlignment.center, spacing: 8, runSpacing: 8, children: [
        _btn(Icons.pause, 'Pause', m.state == RunState.running ? m.pause : null),
        _btn(Icons.play_arrow, 'Resume', m.state == RunState.paused ? m.resume : null),
        FilledButton.icon(
          style: FilledButton.styleFrom(backgroundColor: Colors.white, foregroundColor: AppColors.danger),
          onPressed: () => m.stop(),
          icon: const Icon(Icons.stop_circle),
          label: const Text('Stop'),
        ),
      ]),
    ]);
  }

  Widget _btn(IconData i, String t, VoidCallback? f) => FilledButton.icon(
        style: FilledButton.styleFrom(backgroundColor: Colors.white, foregroundColor: AppColors.primary),
        onPressed: f,
        icon: Icon(i),
        label: Text(t),
      );
}

// ------------------------------------------------------------------- History
void showHistorySheet(BuildContext context, void Function(MassageSession) onReplay) {
  String fmt(DateTime t) {
    const mon = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    final h = t.hour % 12 == 0 ? 12 : t.hour % 12;
    return '${mon[t.month - 1]} ${t.day}, ${t.year} • $h:${t.minute.toString().padLeft(2, '0')} ${t.hour >= 12 ? 'PM' : 'AM'}';
  }

  _showSheet(
    context,
    FutureBuilder<List<MassageSession>>(
      future: BackendService.instance.fetchHistory(),
      builder: (context, snap) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        const _Header(Icons.history, 'Session History'),
        const Text('Sample history (demo) plus sessions from this app run.', style: TextStyle(fontSize: 12, color: Colors.black54)),
        const SizedBox(height: 8),
        if (!snap.hasData) const Center(child: CircularProgressIndicator()),
        for (final s in snap.data ?? const <MassageSession>[])
          Container(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.bg, borderRadius: BorderRadius.circular(14)),
            child: Row(children: [
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('${s.id} • ${fmt(s.time)}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                  Text('${s.intensity.label} • ${s.pattern.label} • ${s.zone.label} • ${s.minutes} min', style: const TextStyle(fontSize: 12)),
                  Text(
                    s.avgHr == null ? 'Heart rate: not available' : 'Avg HR ${s.avgHr} • Peak HR ${s.peakHr ?? '-'}',
                    style: const TextStyle(fontSize: 12, color: Colors.black54),
                  ),
                ]),
              ),
              FilledButton(
                style: FilledButton.styleFrom(minimumSize: const Size(72, 38)),
                onPressed: () {
                  Navigator.pop(context);
                  onReplay(s);
                },
                child: const Text('Replay'),
              ),
            ]),
          ),
      ]),
    ),
  );
}

// ------------------------------------------------------------------ Settings
void showSettingsSheet(BuildContext context) {
  final nav = Navigator.of(context);
  void dialog(String title, Widget body) => showDialog(
        context: context,
        builder: (_) => AlertDialog(
          title: Text(title),
          content: body,
          actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close'))],
        ),
      );

  Widget tile(IconData i, String t, VoidCallback f) => ListTile(
        leading: Icon(i, color: AppColors.deep),
        title: Text(t, style: const TextStyle(fontWeight: FontWeight.w600)),
        trailing: const Icon(Icons.chevron_right),
        onTap: f,
      );

  _showSheet(
    context,
    Column(children: [
      const _Header(Icons.settings, 'Settings'),
      tile(Icons.person, 'Profile', () async {
        final p = await BackendService.instance.fetchProfile(AuthService.instance.email);
        dialog('Profile', Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          for (final e in p.entries) Text('${e.key}: ${e.value}'),
        ]));
      }),
      tile(Icons.notifications, 'Notifications', () => dialog('Notifications', const _NotifySwitch())),
      tile(Icons.help, 'Help & Support', () => dialog('Help & Support', const Text('Support: support@neurocalm.example (placeholder).\nConnect your device from the Connection card, then choose a massage mode.'))),
      tile(Icons.shield_outlined, 'Privacy Policy', () => dialog('Privacy Policy', const Text('Placeholder. Replace with your real privacy policy before release.'))),
      tile(Icons.info, 'About NeuroCalm', () => dialog('About NeuroCalm', const Text('NeuroCalm v1.0.0\nAn intelligent wearable head-massage and physiological monitoring system.'))),
      const SizedBox(height: 8),
      SizedBox(
        width: double.infinity,
        child: FilledButton.icon(
          style: FilledButton.styleFrom(backgroundColor: AppColors.lavender, foregroundColor: AppColors.danger),
          onPressed: () async {
            await MassageService.instance.stop(reason: 'Logged out - session stopped.');
            AuthService.instance.logout();
            nav.pop(); // close sheet
            nav.pushAndRemoveUntil(MaterialPageRoute(builder: (_) => const LoginScreen()), (_) => false);
          },
          icon: const Icon(Icons.logout),
          label: const Text('Logout'),
        ),
      ),
    ]),
  );
}

class _NotifySwitch extends StatefulWidget {
  const _NotifySwitch();
  @override
  State<_NotifySwitch> createState() => _NotifySwitchState();
}

class _NotifySwitchState extends State<_NotifySwitch> {
  bool on = true;
  @override
  Widget build(BuildContext context) => SwitchListTile(
        title: const Text('Session reminders'),
        subtitle: const Text('Preference is local only (demo).'),
        value: on,
        onChanged: (v) => setState(() => on = v),
      );
}

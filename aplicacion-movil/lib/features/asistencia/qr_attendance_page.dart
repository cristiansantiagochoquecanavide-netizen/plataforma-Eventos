import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class QrCheckInException implements Exception {
  const QrCheckInException(this.message);
  final String message;
}

class QrAttendancePage extends StatefulWidget {
  const QrAttendancePage({
    required this.validateQr,
    required this.registerAttendance,
    super.key,
  });

  final Future<Map<String, dynamic>> Function(String codigoQr) validateQr;
  final Future<Map<String, dynamic>> Function(int idInscripcion)
  registerAttendance;

  @override
  State<QrAttendancePage> createState() => _QrAttendancePageState();
}

class _QrAttendancePageState extends State<QrAttendancePage> {
  final MobileScannerController _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    formats: const [BarcodeFormat.qrCode],
  );
  bool _processing = false;
  bool _registering = false;
  String? _error;
  Map<String, dynamic>? _validatedInscription;
  Map<String, dynamic>? _attendance;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _onDetect(BarcodeCapture capture) async {
    if (_processing || _validatedInscription != null || _attendance != null) {
      return;
    }
    final code = capture.barcodes
        .map((barcode) => barcode.rawValue)
        .whereType<String>()
        .map((value) => value.trim())
        .firstWhere((value) => value.isNotEmpty, orElse: () => '');
    if (code.isEmpty) return;

    setState(() {
      _processing = true;
      _error = null;
    });
    try {
      await _controller.stop();
      final inscription = await widget.validateQr(code);
      if (mounted) setState(() => _validatedInscription = inscription);
    } on QrCheckInException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } catch (_) {
      if (mounted) {
        setState(
          () => _error = 'No se pudo validar el código. Intenta nuevamente.',
        );
      }
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _confirmAttendance() async {
    final inscription = _validatedInscription;
    final rawId = inscription?['idInscripcion'];
    if (inscription == null || rawId is! num || _registering) return;

    setState(() {
      _registering = true;
      _error = null;
    });
    try {
      final attendance = await widget.registerAttendance(rawId.toInt());
      if (mounted) setState(() => _attendance = attendance);
    } on QrCheckInException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } catch (_) {
      if (mounted) {
        setState(
          () => _error =
              'No se pudo registrar la asistencia. Intenta nuevamente.',
        );
      }
    } finally {
      if (mounted) setState(() => _registering = false);
    }
  }

  Future<void> _scanAgain() async {
    setState(() {
      _validatedInscription = null;
      _attendance = null;
      _error = null;
    });
    await _controller.start();
  }

  String _formatDate(dynamic rawValue) {
    final date = DateTime.parse(rawValue as String).toLocal();
    String twoDigits(int part) => part.toString().padLeft(2, '0');
    return '${twoDigits(date.day)}/${twoDigits(date.month)}/${date.year} '
        '${twoDigits(date.hour)}:${twoDigits(date.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    final inscription = _validatedInscription;
    final attendance = _attendance;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Control de acceso'),
        actions: [
          if (inscription == null && attendance == null)
            IconButton(
              tooltip: 'Cambiar cámara',
              onPressed: _controller.switchCamera,
              icon: const Icon(Icons.cameraswitch_rounded),
            ),
          if (inscription == null && attendance == null)
            ValueListenableBuilder(
              valueListenable: _controller,
              builder: (context, state, _) => IconButton(
                tooltip: state.torchState == TorchState.on
                    ? 'Apagar linterna'
                    : 'Encender linterna',
                onPressed: _controller.toggleTorch,
                icon: Icon(
                  state.torchState == TorchState.on
                      ? Icons.flash_on_rounded
                      : Icons.flash_off_rounded,
                ),
              ),
            ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                attendance != null
                    ? 'La asistencia quedó registrada.'
                    : inscription != null
                    ? 'Código válido. Revisa los datos antes de confirmar el check-in.'
                    : 'Enfoca el código QR de la acreditación para validar la inscripción.',
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 20),
              if (inscription == null && attendance == null)
                ClipRRect(
                  borderRadius: BorderRadius.circular(20),
                  child: AspectRatio(
                    aspectRatio: 1,
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        MobileScanner(
                          controller: _controller,
                          onDetect: _onDetect,
                        ),
                        IgnorePointer(
                          child: Center(
                            child: Container(
                              width: 230,
                              height: 230,
                              decoration: BoxDecoration(
                                border: Border.all(
                                  color: Colors.white,
                                  width: 3,
                                ),
                                borderRadius: BorderRadius.circular(20),
                              ),
                            ),
                          ),
                        ),
                        if (_processing)
                          ColoredBox(
                            color: Colors.black54,
                            child: const Center(
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  CircularProgressIndicator(
                                    color: Colors.white,
                                  ),
                                  SizedBox(height: 12),
                                  Text(
                                    'Validando código…',
                                    style: TextStyle(color: Colors.white),
                                  ),
                                ],
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              if (inscription != null && attendance == null) ...[
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Inscripción validada',
                          style: TextStyle(fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'Participante: ${inscription['participante'] ?? ''}',
                        ),
                        Text(
                          'Evento: ${inscription['nombreEvento'] ?? 'Evento'}',
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                FilledButton.icon(
                  onPressed: _registering ? null : _confirmAttendance,
                  icon: _registering
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.how_to_reg_rounded),
                  label: Text(
                    _registering
                        ? 'Registrando...'
                        : 'Confirmar y registrar asistencia',
                  ),
                ),
                TextButton.icon(
                  onPressed: _registering ? null : _scanAgain,
                  icon: const Icon(Icons.qr_code_scanner_rounded),
                  label: const Text('Escanear otro código'),
                ),
              ],
              if (_error != null) ...[
                const SizedBox(height: 16),
                Text(
                  _error!,
                  key: const Key('qr-attendance-error'),
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  onPressed: _processing || _registering ? null : _scanAgain,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Volver a escanear'),
                ),
              ],
              if (attendance != null) ...[
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(Icons.verified_rounded, color: Colors.green),
                            SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Asistencia registrada correctamente',
                                style: TextStyle(fontWeight: FontWeight.w700),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),
                        Text(
                          'Participante: ${attendance['participante'] ?? ''}',
                        ),
                        Text(
                          'Evento: ${attendance['nombreEvento'] ?? 'Evento'}',
                        ),
                        Text(
                          'Fecha y hora: ${_formatDate(attendance['fechaHora'])}',
                        ),
                        Text(
                          'Registrado por: ${attendance['nombreRegistradoPor'] ?? ''}',
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                FilledButton.icon(
                  onPressed: _scanAgain,
                  icon: const Icon(Icons.qr_code_scanner_rounded),
                  label: const Text('Escanear siguiente acreditación'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

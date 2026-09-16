import 'package:flutter/material.dart';

import '../../main.dart';
import '../models/convocatoria_model.dart';
import '../models/postulacion_staff_model.dart';

class DetalleConvocatoriaPage extends StatefulWidget {
  const DetalleConvocatoriaPage({
    required this.session,
    required this.idConvocatoria,
    super.key,
  });

  final SessionController session;
  final int idConvocatoria;

  @override
  State<DetalleConvocatoriaPage> createState() =>
      _DetalleConvocatoriaPageState();
}

class _DetalleConvocatoriaPageState extends State<DetalleConvocatoriaPage> {
  ConvocatoriaModel? _convocatoria;
  PostulacionStaffModel? _postulacion;
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _handleError(Object error) {
    if (!mounted) return;
    if (error is ApiException && error.status == 401) {
      Navigator.of(context).popUntil((route) => route.isFirst);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(error.message)));
      return;
    }
    setState(
      () => _error = error is ApiException
          ? error.message
          : 'No se pudo completar la solicitud. Intenta nuevamente.',
    );
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final convocatoria = await widget.session.obtenerConvocatoria(
        widget.idConvocatoria,
      );
      if (!mounted) return;
      setState(() => _convocatoria = convocatoria);
      final postulacion = await widget.session.consultarPostulacion(
        widget.idConvocatoria,
      );
      if (mounted) {
        setState(() {
          _convocatoria = convocatoria;
          _postulacion = postulacion;
        });
      }
    } catch (error) {
      _handleError(error);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _postularse() async {
    if (_saving || _postulacion != null) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final postulacion = await widget.session.postularse(
        widget.idConvocatoria,
      );
      if (!mounted) return;
      setState(() => _postulacion = postulacion);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Tu postulacion se registro correctamente.'),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      if (error is ApiException && error.status == 401) {
        _handleError(error);
      } else {
        // Un 400 puede ser un duplicado o un cambio de estado; una perdida de
        // conexion tambien puede ocurrir despues de que el servidor guarde.
        await _load();
        if (mounted) _handleError(error);
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _retirar() async {
    if (_saving || _postulacion?.estado != 'PENDIENTE') return;
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Retirar postulacion'),
        content: const Text(
          'Se conservara el historial y no podras volver a postularte a esta convocatoria.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Retirar'),
          ),
        ],
      ),
    );
    if (confirmar != true || !mounted) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final postulacion = await widget.session.retirarPostulacion(
        _postulacion!.id,
      );
      if (!mounted) return;
      setState(() => _postulacion = postulacion);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Postulacion retirada.')));
    } catch (error) {
      if (!mounted) return;
      if (error is! ApiException || error.status != 401) await _load();
      _handleError(error);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final convocatoria = _convocatoria;
    final postulacion = _postulacion;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle de convocatoria'),
        actions: [
          IconButton(
            onPressed: _loading || _saving ? null : _load,
            tooltip: 'Actualizar',
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(24),
              children: [
                if (_error != null) ...[
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      _error!,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                      ),
                    ),
                  ),
                  TextButton(
                    onPressed: _saving ? null : _load,
                    child: const Text('Actualizar estado'),
                  ),
                  const SizedBox(height: 16),
                ],
                if (convocatoria != null) ...[
                  Text(
                    convocatoria.nombreEvento,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 8,
                    children: [
                      Chip(label: Text(convocatoria.tipoPersonal)),
                      Chip(label: Text(convocatoria.estado)),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(convocatoria.descripcion),
                  const SizedBox(height: 24),
                  Text(
                    'Personas requeridas: ${convocatoria.cantidadRequerida}',
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Fecha limite: ${fechaConvocatoria(convocatoria.fechaLimite)}',
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Publicada: ${fechaConvocatoria(convocatoria.fechaCreacion.toLocal())}',
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Actualizada: ${fechaConvocatoria(convocatoria.fechaActualizacion.toLocal())}',
                  ),
                  const SizedBox(height: 24),
                  if (postulacion != null) ...[
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(18),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Tu postulacion: ${postulacion.estado}',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                            const SizedBox(height: 8),
                            Text(
                              'Registrada: ${fechaConvocatoria(postulacion.fechaPostulacion.toLocal())}',
                            ),
                            if (postulacion.estado == 'RETIRADA')
                              const Text(
                                'Ya no participas en esta convocatoria. No se permiten nuevas postulaciones.',
                              ),
                          ],
                        ),
                      ),
                    ),
                    if (postulacion.estado == 'PENDIENTE')
                      OutlinedButton(
                        onPressed: _saving || _error != null ? null : _retirar,
                        child: Text(
                          _saving ? 'Procesando...' : 'Retirar postulacion',
                        ),
                      ),
                  ] else ...[
                    if (!convocatoria.vigente)
                      const Text(
                        'Esta convocatoria ya no admite postulaciones.',
                      ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed:
                          _saving || !convocatoria.vigente || _error != null
                          ? null
                          : _postularse,
                      icon: _saving
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.person_add_alt),
                      label: Text(_saving ? 'Enviando...' : 'Postularme'),
                    ),
                  ],
                ],
              ],
            ),
    );
  }
}

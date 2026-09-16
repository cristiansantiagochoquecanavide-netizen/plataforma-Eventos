import 'package:flutter/material.dart';

import '../../main.dart';
import '../models/convocatoria_model.dart';
import 'detalle_convocatoria_page.dart';

class ConvocatoriasPage extends StatefulWidget {
  const ConvocatoriasPage({required this.session, super.key});

  final SessionController session;

  @override
  State<ConvocatoriasPage> createState() => _ConvocatoriasPageState();
}

class _ConvocatoriasPageState extends State<ConvocatoriasPage> {
  List<ConvocatoriaModel> _convocatorias = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await widget.session.listarConvocatorias();
      if (mounted) {
        setState(
          () => _convocatorias = result
              .where((item) => item.estado == 'ABIERTA')
              .toList(),
        );
      }
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error is ApiException
              ? error.message
              : 'No se pudieron cargar las convocatorias.',
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Convocatorias'),
      actions: [
        IconButton(
          onPressed: _loading ? null : _load,
          tooltip: 'Actualizar',
          icon: const Icon(Icons.refresh),
        ),
      ],
    ),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(20),
              children: [
                Text(
                  'Forma parte del equipo',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 8),
                const Text(
                  'Encuentra una convocatoria y participa en nuestros eventos.',
                ),
                const SizedBox(height: 20),
                if (_error != null) ...[
                  Text(_error!),
                  TextButton(onPressed: _load, child: const Text('Reintentar')),
                ] else if (_convocatorias.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                      'No hay convocatorias abiertas por el momento.',
                    ),
                  )
                else
                  ..._convocatorias.map(
                    (item) => Card(
                      child: InkWell(
                        borderRadius: BorderRadius.circular(12),
                        onTap: () async {
                          await Navigator.of(context).push(
                            MaterialPageRoute<void>(
                              builder: (_) => DetalleConvocatoriaPage(
                                session: widget.session,
                                idConvocatoria: item.id,
                              ),
                            ),
                          );
                          if (mounted && widget.session.user != null) {
                            await _load();
                          }
                        },
                        child: Padding(
                          padding: const EdgeInsets.all(18),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                item.nombreEvento,
                                style: Theme.of(context).textTheme.titleLarge,
                              ),
                              const SizedBox(height: 8),
                              Chip(label: Text(item.tipoPersonal)),
                              const SizedBox(height: 8),
                              Text(
                                item.descripcion,
                                maxLines: 3,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 16),
                              Text(
                                'Fecha limite: ${fechaConvocatoria(item.fechaLimite)}',
                              ),
                              if (!item.vigente) const Text('Plazo vencido'),
                              const SizedBox(height: 8),
                              const Text('Ver convocatoria →'),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
  );
}

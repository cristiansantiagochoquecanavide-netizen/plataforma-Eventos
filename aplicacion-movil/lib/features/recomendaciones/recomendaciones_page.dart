import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../main.dart';
import 'recomendaciones_model.dart';

class RecomendacionesIaPage extends StatefulWidget {
  const RecomendacionesIaPage({
    required this.session,
    this.onGoToEventos,
    super.key,
  });

  final SessionController session;
  final VoidCallback? onGoToEventos;

  @override
  State<RecomendacionesIaPage> createState() => _RecomendacionesIaPageState();
}

class _RecomendacionesIaPageState extends State<RecomendacionesIaPage> {
  bool _isLoading = false;
  String? _error;
  List<EventoRecomendadoModel> _eventos = [];
  int? _inscribiendoEventoId;

  @override
  void initState() {
    super.initState();
    _cargarRecomendaciones();
  }

  Future<void> _cargarRecomendaciones() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final items = await widget.session.listRecommendedEvents();
      if (mounted) {
        setState(() {
          _eventos = items;
          _isLoading = false;
        });
      }
    } on ApiException catch (e) {
      if (mounted) {
        setState(() {
          _error = e.message;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString().replaceAll('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _inscribir(EventoRecomendadoModel evento) async {
    setState(() => _inscribiendoEventoId = evento.idEvento);
    try {
      await widget.session.inscribirAEvento(evento.idEvento);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('¡Inscripción confirmada para "${evento.titulo}"!'),
            backgroundColor: AppColors.primary,
            behavior: SnackBarBehavior.floating,
          ),
        );
        // Recargar recomendaciones para actualizar eventos disponibles
        _cargarRecomendaciones();
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error al inscribirse: ${e.message}'),
            backgroundColor: AppColors.danger,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Error al inscribirse: ${e.toString().replaceAll('Exception: ', '')}',
            ),
            backgroundColor: AppColors.danger,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _inscribiendoEventoId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            Icon(Icons.auto_awesome_rounded, color: AppColors.amber, size: 22),
            SizedBox(width: 8),
            Text('Recomendados para ti'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Actualizar sugerencias',
            onPressed: _isLoading ? null : _cargarRecomendaciones,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _cargarRecomendaciones,
        child: _buildBody(theme),
      ),
    );
  }

  Widget _buildBody(ThemeData theme) {
    if (_isLoading && _eventos.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text(
              'Analizando tus preferencias con IA...',
              style: TextStyle(fontWeight: FontWeight.w500),
            ),
          ],
        ),
      );
    }

    if (_error != null && _eventos.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.error_outline_rounded,
                size: 52,
                color: AppColors.danger,
              ),
              const SizedBox(height: 16),
              Text(
                'No pudimos cargar tus recomendaciones',
                style: theme.textTheme.titleMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                _error!,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: AppColors.muted,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 20),
              FilledButton.icon(
                onPressed: _cargarRecomendaciones,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      );
    }

    if (_eventos.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.recommend_rounded,
                size: 58,
                color: AppColors.accent,
              ),
              const SizedBox(height: 16),
              Text(
                'No hay nuevos eventos por sugerir',
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                'Estás al día con todos los eventos de la organización o ya te encuentras inscrito en ellos.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: AppColors.muted,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              if (widget.onGoToEventos != null)
                FilledButton.tonalIcon(
                  onPressed: widget.onGoToEventos,
                  icon: const Icon(Icons.event_note_rounded),
                  label: const Text('Ver catálogo completo'),
                ),
            ],
          ),
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      children: [
        // AI Banner
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [AppColors.forest, AppColors.navy],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: AppColors.forest.withValues(alpha: .16),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.15),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.psychology_rounded,
                  color: AppColors.amber,
                  size: 26,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Sugerencias Inteligentes',
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Modeladas según tu historial de asistencia e intereses temáticos en la organización.',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: .84),
                        fontSize: 12,
                        height: 1.3,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),

        // List of recommended cards
        ..._eventos.map((ev) => _buildCard(context, ev, theme)),
      ],
    );
  }

  Widget _buildCard(
    BuildContext context,
    EventoRecomendadoModel ev,
    ThemeData theme,
  ) {
    final bool isInscribiendo = _inscribiendoEventoId == ev.idEvento;
    final int afinidadPct = ev.porcentajeAfinidad;

    Color badgeColor;
    if (afinidadPct >= 85) {
      badgeColor = AppColors.forest;
    } else if (afinidadPct >= 65) {
      badgeColor = AppColors.olive;
    } else {
      badgeColor = AppColors.burgundy;
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top badges
            Row(
              children: [
                Chip(
                  label: Text(
                    ev.nombreCategoria,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  padding: EdgeInsets.zero,
                  visualDensity: VisualDensity.compact,
                  backgroundColor: AppColors.sage,
                ),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: badgeColor.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: badgeColor.withOpacity(0.5)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.bolt_rounded, size: 14, color: badgeColor),
                      const SizedBox(width: 4),
                      Text(
                        '$afinidadPct% match',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: badgeColor,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),

            // Event Title
            Text(
              ev.titulo,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
                fontSize: 17,
              ),
            ),
            const SizedBox(height: 6),

            // Location & Date
            Row(
              children: [
                const Icon(
                  Icons.location_on_outlined,
                  size: 16,
                  color: AppColors.muted,
                ),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    ev.ubicacion,
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.muted,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(
                  Icons.calendar_today_outlined,
                  size: 15,
                  color: AppColors.muted,
                ),
                const SizedBox(width: 5),
                Text(
                  _formatearFecha(ev.fechaInicio),
                  style: const TextStyle(fontSize: 13, color: AppColors.muted),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // AI Reason Box
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.sand,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: AppColors.amber.withValues(alpha: .5),
                ),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.auto_awesome,
                    size: 18,
                    color: AppColors.amber,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      ev.motivoRecomendacion,
                      style: TextStyle(
                        fontSize: 13,
                        color: AppColors.forest,
                        fontWeight: FontWeight.w500,
                        height: 1.3,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),

            // Tags
            if (ev.etiquetas.isNotEmpty) ...[
              Wrap(
                spacing: 6,
                runSpacing: 4,
                children: ev.etiquetas
                    .map(
                      (t) => Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.sage,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          '#$t',
                          style: const TextStyle(
                            fontSize: 11,
                            color: AppColors.forest,
                          ),
                        ),
                      ),
                    )
                    .toList(),
              ),
              const SizedBox(height: 14),
            ],

            // Action Row
            Row(
              children: [
                if (ev.aforoMaximo != null && ev.aforoMaximo! > 0)
                  Text(
                    '${ev.inscritos}/${ev.aforoMaximo} inscritos',
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                      color: AppColors.muted,
                    ),
                  )
                else
                  Text(
                    '${ev.inscritos} inscritos',
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                      color: AppColors.muted,
                    ),
                  ),
                const Spacer(),
                FilledButton.icon(
                  onPressed: isInscribiendo ? null : () => _inscribir(ev),
                  icon: isInscribiendo
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.bookmark_add_outlined, size: 18),
                  label: Text(
                    isInscribiendo ? 'Inscribiendo...' : 'Inscribirme',
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatearFecha(DateTime fecha) {
    final meses = [
      'Ene',
      'Feb',
      'Mar',
      'Abr',
      'May',
      'Jun',
      'Jul',
      'Ago',
      'Sep',
      'Oct',
      'Nov',
      'Dic',
    ];
    final hora = fecha.hour.toString().padLeft(2, '0');
    final min = fecha.minute.toString().padLeft(2, '0');
    return '${fecha.day} ${meses[fecha.month - 1]} ${fecha.year} - $hora:$min';
  }
}

class EventoRecomendadoModel {
  const EventoRecomendadoModel({
    required this.idEvento,
    required this.titulo,
    required this.descripcion,
    required this.fechaInicio,
    this.fechaFin,
    required this.ubicacion,
    required this.nombreCategoria,
    this.aforoMaximo,
    required this.inscritos,
    required this.afinidad,
    required this.motivoRecomendacion,
    required this.etiquetas,
  });

  final int idEvento;
  final String titulo;
  final String descripcion;
  final DateTime fechaInicio;
  final DateTime? fechaFin;
  final String ubicacion;
  final String nombreCategoria;
  final int? aforoMaximo;
  final int inscritos;
  final double afinidad;
  final String motivoRecomendacion;
  final List<String> etiquetas;

  int get porcentajeAfinidad => (afinidad * 100).clamp(0, 100).toInt();

  factory EventoRecomendadoModel.fromJson(Map<String, dynamic> json) {
    return EventoRecomendadoModel(
      idEvento: json['idEvento'] as int,
      titulo: json['titulo'] as String? ?? 'Evento',
      descripcion: json['descripcion'] as String? ?? '',
      fechaInicio: DateTime.parse(json['fechaInicio'] as String),
      fechaFin: json['fechaFin'] != null ? DateTime.tryParse(json['fechaFin'] as String) : null,
      ubicacion: json['ubicacion'] as String? ?? 'Por definir',
      nombreCategoria: json['nombreCategoria'] as String? ?? 'General',
      aforoMaximo: json['aforoMaximo'] as int?,
      inscritos: json['inscritos'] as int? ?? 0,
      afinidad: json['afinidad'] != null ? (json['afinidad'] as num).toDouble() : 0.8,
      motivoRecomendacion: json['motivoRecomendacion'] as String? ?? 'Evento recomendado especialmente para ti.',
      etiquetas: (json['etiquetas'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const <String>[],
    );
  }
}

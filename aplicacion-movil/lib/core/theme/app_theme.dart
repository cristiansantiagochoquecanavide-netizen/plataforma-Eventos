import 'package:flutter/material.dart';

abstract final class AppColors {
  static const forest = Color(0xff17312c);
  static const primary = Color(0xff0c5d4c);
  static const accent = Color(0xff16816b);
  static const amber = Color(0xffe4ad38);
  static const canvas = Color(0xffeef2ee);
  static const ink = Color(0xff17211f);
  static const muted = Color(0xff63716b);
  static const danger = Color(0xffa32c20);
}

ThemeData appTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: AppColors.primary,
    brightness: Brightness.light,
  );
  return ThemeData(
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.canvas,
    useMaterial3: true,
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 17),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: Color(0xffc8d1ca)),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: Color(0xffc8d1ca)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: AppColors.accent, width: 2),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(54),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        textStyle: const TextStyle(fontWeight: FontWeight.w800),
      ),
    ),
    snackBarTheme: const SnackBarThemeData(
      backgroundColor: AppColors.forest,
      contentTextStyle: TextStyle(color: Colors.white),
    ),
  );
}

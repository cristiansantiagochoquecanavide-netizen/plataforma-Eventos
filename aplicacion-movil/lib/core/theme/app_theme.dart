import 'package:flutter/material.dart';

abstract final class AppColors {
  static const forest = Color(0xff173b32);
  static const primary = Color(0xff285848);
  static const accent = Color(0xff747b58);
  static const amber = Color(0xffb18a4a);
  static const navy = Color(0xff29384a);
  static const burgundy = Color(0xff713b43);
  static const olive = Color(0xff777b56);
  static const canvas = Color(0xfff2efe6);
  static const paper = Color(0xfffbf8f0);
  static const sage = Color(0xffe5e8dc);
  static const line = Color(0xffd9d1c2);
  static const ink = Color(0xff27312d);
  static const muted = Color(0xff746f64);
  static const danger = Color(0xffa94e43);
  static const sand = Color(0xfff1e8d5);
}

ThemeData appTheme() {
  final scheme =
      ColorScheme.fromSeed(
        seedColor: AppColors.forest,
        brightness: Brightness.light,
        primary: AppColors.forest,
        secondary: AppColors.amber,
        surface: AppColors.paper,
        error: AppColors.danger,
      ).copyWith(
        onSurface: AppColors.ink,
        outline: AppColors.line,
        outlineVariant: AppColors.line.withValues(alpha: .72),
        surfaceContainerLowest: AppColors.paper,
        surfaceContainerLow: AppColors.canvas,
        primaryContainer: AppColors.sage,
        onPrimaryContainer: AppColors.forest,
        secondaryContainer: AppColors.sand,
        onSecondaryContainer: AppColors.forest,
      );

  const titleStyle = TextStyle(
    color: AppColors.ink,
    fontFamily: 'serif',
    fontWeight: FontWeight.w800,
    letterSpacing: -.2,
  );

  return ThemeData(
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.canvas,
    useMaterial3: true,
    visualDensity: VisualDensity.standard,
    textTheme: TextTheme(
      displayLarge: titleStyle.copyWith(
        fontSize: 32,
        height: 1.1,
        letterSpacing: -1.1,
      ),
      displayMedium: titleStyle.copyWith(
        fontSize: 28,
        height: 1.12,
        letterSpacing: -.8,
      ),
      headlineLarge: titleStyle.copyWith(
        fontSize: 24,
        height: 1.16,
        letterSpacing: -.6,
      ),
      headlineMedium: titleStyle.copyWith(
        fontSize: 21,
        height: 1.2,
        letterSpacing: -.45,
      ),
      headlineSmall: titleStyle.copyWith(
        fontSize: 19,
        height: 1.22,
        letterSpacing: -.25,
      ),
      titleLarge: titleStyle.copyWith(fontSize: 17, height: 1.25),
      titleMedium: titleStyle.copyWith(
        fontSize: 15,
        height: 1.3,
        fontWeight: FontWeight.w700,
      ),
      titleSmall: titleStyle.copyWith(
        fontSize: 13,
        height: 1.3,
        fontWeight: FontWeight.w700,
      ),
      bodyLarge: TextStyle(color: AppColors.ink, fontSize: 15, height: 1.45),
      bodyMedium: TextStyle(color: AppColors.ink, fontSize: 13, height: 1.45),
      bodySmall: TextStyle(color: AppColors.muted, fontSize: 12, height: 1.4),
      labelLarge: TextStyle(
        color: AppColors.forest,
        fontSize: 13,
        fontWeight: FontWeight.w700,
      ),
      labelMedium: TextStyle(
        color: AppColors.muted,
        fontSize: 11,
        fontWeight: FontWeight.w700,
      ),
      labelSmall: TextStyle(
        color: AppColors.muted,
        fontSize: 10,
        fontWeight: FontWeight.w800,
        letterSpacing: .8,
      ),
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.canvas,
      foregroundColor: AppColors.ink,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
      toolbarHeight: 56,
      titleTextStyle: TextStyle(
        color: AppColors.ink,
        fontFamily: 'serif',
        fontSize: 20,
        fontWeight: FontWeight.w800,
        letterSpacing: -.35,
      ),
      iconTheme: IconThemeData(color: AppColors.forest, size: 22),
    ),
    cardTheme: CardThemeData(
      color: AppColors.paper,
      surfaceTintColor: Colors.transparent,
      elevation: 1.5,
      shadowColor: AppColors.navy.withValues(alpha: .08),
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: const BorderSide(color: AppColors.line),
      ),
    ),
    dividerTheme: const DividerThemeData(
      color: AppColors.line,
      thickness: 1,
      space: 22,
    ),
    drawerTheme: const DrawerThemeData(
      backgroundColor: AppColors.paper,
      surfaceTintColor: Colors.transparent,
      width: 320,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.horizontal(right: Radius.circular(26)),
      ),
    ),
    listTileTheme: const ListTileThemeData(
      iconColor: AppColors.muted,
      textColor: AppColors.ink,
      contentPadding: EdgeInsets.symmetric(horizontal: 14),
      minLeadingWidth: 38,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: AppColors.paper,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      height: 70,
      indicatorColor: AppColors.sage,
      labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
      labelTextStyle: WidgetStateProperty.resolveWith(
        (states) => TextStyle(
          color: states.contains(WidgetState.selected)
              ? AppColors.forest
              : AppColors.muted,
          fontSize: 11,
          fontWeight: states.contains(WidgetState.selected)
              ? FontWeight.w800
              : FontWeight.w600,
        ),
      ),
      iconTheme: WidgetStateProperty.resolveWith(
        (states) => IconThemeData(
          color: states.contains(WidgetState.selected)
              ? AppColors.primary
              : AppColors.muted,
          size: 22,
        ),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: AppColors.paper,
      contentPadding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(15),
        borderSide: const BorderSide(color: AppColors.line),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(15),
        borderSide: const BorderSide(color: AppColors.line),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(15),
        borderSide: const BorderSide(color: AppColors.accent, width: 1.7),
      ),
      labelStyle: const TextStyle(
        color: AppColors.muted,
        fontWeight: FontWeight.w600,
        fontSize: 13,
      ),
      hintStyle: const TextStyle(color: AppColors.muted, fontSize: 13),
      floatingLabelStyle: const TextStyle(
        color: AppColors.primary,
        fontWeight: FontWeight.w700,
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(48),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w800),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.primary,
        side: const BorderSide(color: AppColors.line),
        minimumSize: const Size.fromHeight(46),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700),
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: AppColors.sage,
      selectedColor: AppColors.sage,
      disabledColor: AppColors.line,
      side: BorderSide.none,
      labelStyle: const TextStyle(
        color: AppColors.forest,
        fontSize: 12,
        fontWeight: FontWeight.w700,
      ),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
    ),
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: AppColors.paper,
      surfaceTintColor: Colors.transparent,
      modalBackgroundColor: AppColors.paper,
      showDragHandle: true,
    ),
    snackBarTheme: const SnackBarThemeData(
      backgroundColor: AppColors.forest,
      contentTextStyle: TextStyle(color: Colors.white),
      behavior: SnackBarBehavior.floating,
    ),
  );
}

#!/bin/bash
set -e
D=/home/user/Doubao/chats/38445152083111682/ShadowBrowser/app/src/main/res
echo "res dir: $D"
mkdir -p "$D/drawable" "$D/mipmap-anydpi-v26"

# ---------- shape drawables ----------
cat > "$D/drawable/bg_pill_green.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="16dp"/>
    <solid android:color="@color/proxy_connected"/>
</shape>
EOF
cat > "$D/drawable/bg_pill_amber.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="16dp"/>
    <solid android:color="#F9A825"/>
</shape>
EOF
cat > "$D/drawable/bg_pill_red.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="16dp"/>
    <solid android:color="@color/proxy_error"/>
</shape>
EOF
cat > "$D/drawable/bg_pill_gray.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="16dp"/>
    <solid android:color="@color/proxy_disconnected"/>
</shape>
EOF
cat > "$D/drawable/bg_card.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="8dp"/>
    <solid android:color="#E8F5E9"/>
</shape>
EOF
cat > "$D/drawable/bg_surface.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="8dp"/>
    <solid android:color="@color/surface"/>
    <stroke android:width="1dp" android:color="@color/divider"/>
</shape>
EOF
cat > "$D/drawable/bg_btn_primary.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="8dp"/>
    <solid android:color="@color/primary"/>
</shape>
EOF
cat > "$D/drawable/bg_btn_outline.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="8dp"/>
    <stroke android:width="1dp" android:color="@color/accent"/>
</shape>
EOF
cat > "$D/drawable/bg_search_box.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="22dp"/>
    <solid android:color="#F5F5F5"/>
</shape>
EOF
cat > "$D/drawable/dot_active.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="@color/primary"/>
</shape>
EOF
cat > "$D/drawable/dot_inactive.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#D0D0D0"/>
</shape>
EOF

# ---------- vector icons (24x24) ----------
GRAY="#616161"
icon() {
  local name="$1" path="$2"
  cat > "$D/drawable/$name.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="$GRAY" android:pathData="$path"/>
</vector>
EOF
}

icon ic_back "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"
icon ic_forward "M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8z"
icon ic_home "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"
icon ic_menu "M3,18h18v-2H3v2zm0,-5h18v-2H3v2zm0,-7v2h18V6H3z"
icon ic_close "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
icon ic_search "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"
icon ic_delete "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"
icon ic_add "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"
icon ic_settings "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94 0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.9 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6 3.6,1.62 3.6,3.6 -1.62,3.6 -3.6,3.6z"
icon ic_node "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM13,7h-2v4H7v2h4v4h2v-4h4v-2h-4V7z"
icon ic_latency "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM13,7h-2v5l4.28,2.54 1,-1.73 -3.28,-1.95V7z"
icon ic_import "M5,20h14v-2H5v2zM19,9h-4V3H9v6H5l7,7 7,-7z"
icon ic_history "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13,8H12z"
icon ic_bookmark "M17,3H7c-1.1,0 -2,0.9 -2,2v16l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z"
icon ic_add_bookmark "M17,3H7c-1.1,0 -2,0.9 -2,2v16l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2zM16,8h-2v2h2v2h2v-2h2V8h-2V6h-2v2z"
icon ic_share "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z"
icon ic_download "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z"
icon ic_incognito "M12,7c-2.76,0 -5,2.24 -5,5 0,0.65 0.13,1.26 0.36,1.83 -0.54,0.11 -1.05,0.32 -1.5,0.62C5.48,13.6 5,12.84 5,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7c0,0.84 -0.48,1.6 -1.14,2.45 -0.45,-0.3 -0.96,-0.51 -1.5,-0.62 0.23,-0.57 0.36,-1.18 0.36,-1.83 0,-2.76 -2.24,-5 -5,-5zM12,8c-2.21,0 -4,1.79 -4,4 0,0.34 0.04,0.67 0.11,0.99C8.93,12.36 9.44,12 10,12h4c0.56,0 1.07,0.36 1.89,0.99 0.07,-0.32 0.11,-0.65 0.11,-0.99 0,-2.21 -1.79,-4 -4,-4zM4,17c0,1.66 1.34,3 3,3s3,-1.34 3,-3c0,-1.66 -1.34,-3 -3,-3S4,15.34 4,17zM17,17c0,1.66 1.34,3 3,3s3,-1.34 3,-3c0,-1.66 -1.34,-3 -3,-3S17,15.34 17,17z"
icon ic_desktop "M21,2H3c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h7l-2,3v1h8v-1l-2,-3h7c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2z"
icon ic_toolbox "M20,8h-3V4c0,-1.1 -0.9,-2 -2,-2H9C7.9,2 7,2.9 7,4v4H4c-1.1,0 -2,0.9 -2,2v8c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2v-8c0,-1.1 -0.9,-2 -2,-2zM9,4h6v4H9V4z"
icon ic_find "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"
icon ic_save "M17,3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V7l-4,-4zM12,19c-1.66,0 -3,-1.34 -3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3zM15,9H5V5h10v4z"
icon ic_offline "M5,10h4V3h6v7h4l-7,7 -7,-7zM5,18v2h14v-2H5z"
icon ic_translate "M12.87,15.07l-2.54,-2.51 0.03,-0.03c1.74,-1.94 2.98,-4.17 3.71,-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5,7.92 10.44,9.75 9,11.35 8.07,10.32 7.3,9.19 6.69,8h-2c0.73,1.63 1.73,3.17 2.98,4.56l-5.09,5.02L4,19l5,-5 3.11,3.11 0.76,-2.04zM18.5,10h-2L12,22h2l1.12,-3h4.75L21,22h2l-4.5,-12zM15.36,17l1.89,-5.05 0.01,-0.01 1.89,5.06h-3.79z"
icon ic_source "M9.4,16.6L4.8,12l4.6,-4.6L8,6l-6,6 6,6 1.4,-1.4zM14.6,6l-1.2,1.4L19.2,12l-4.6,4.6L16,18l6,-6 -6,-6z"
icon ic_fullscreen "M7,14H5v5h5v-2H7v-3zM5,10h2V7h3V5H5v5zM17,17h-3v2h5v-5h-2v3zM14,5v2h3v3h2V5h-5z"
icon ic_image "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z"
icon ic_sniff "M12,3C7,3 2.9,5.6 0.5,9.5l1.6,1.2C4,7.3 7.7,5 12,5s8,2.3 9.9,5.7l1.6,-1.2C21.1,5.6 17,3 12,3zM12,7c-3.5,0 -6.7,1.5 -8.9,3.9l1.6,1.2C6.5,10.2 9.1,9 12,9s5.5,1.2 7.3,3.1l1.6,-1.2C18.7,8.5 15.5,7 12,7zM12,11c-2.2,0 -4.2,1 -5.6,2.5l1.6,1.2C9.1,13.6 10.4,13 12,13s2.9,0.6 4,1.7l1.6,-1.2C16.2,12 14.2,11 12,11zM12,15c-1.1,0 -2.1,0.5 -2.8,1.2L12,19l2.8,-2.8C14.1,15.5 13.1,15 12,15z"
icon ic_identity "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"
icon ic_netlog "M4,6h16V4H4v2zM4,12h16v-2H4v2zM4,18h16v-2H4v2z"
icon ic_night "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3z"
icon ic_qr "M3,11h8V3H3v8zM5,5h4v4H5V5zM3,21h8v-8H3v8zM5,15h4v4H5v-4zM13,3v8h8V3h-8zM15,5h4v4h-4V5zM19,13h2v2h-2zM13,13h2v2h-2zM13,19h2v2h-2zM17,17h2v2h-2zM19,17h2v2h-2zM19,21h2v2h-2z"
icon ic_home_shortcut "M12,3C7.03,3 3,7.03 3,12v9h7v-6h4v6h7v-9c0,-4.97 -4.03,-9 -9,-9z"
icon ic_tts "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
icon ic_ai "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4 6.5,9.5 1,12l5.5,2.5L9,20l2.5,-5.5L17,12l-5.5,-2.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z"
icon ic_orientation "M6.35,6.35L4,4v6h6L7.06,7.06C8.26,5.87 10.04,5 12,5c3.31,0 6,2.69 6,6h2c0,-4.42 -3.58,-8 -8,-8 -2.21,0 -4.21,0.9 -5.65,2.35zM18.65,17.65L20,20v-6h-6l2.94,2.94C17.74,18.13 15.96,19 14,19c-3.31,0 -6,-2.69 -6,-6H6c0,4.42 3.58,8 8,8 2.21,0 4.21,-0.9 5.65,-2.35z"
icon ic_adblock "M12,2L4,5v6c0,5.55 3.84,10.74 8,12 4.16,-1.26 8,-6.45 8,-12V5l-8,-3zM12,4l6,2.25V11c0,4.52 -2.98,8.69 -6,9.93C8.98,19.69 6,15.52 6,11V6.25L12,4z"
icon ic_mark_ad "M13,2L4.13,19h4L13,11.05 18.87,19H22L13,2zM13,8.11l2.5,4.39h-5L13,8.11z"
icon ic_fontsize "M9,4v3h5v12h3V7h5V4H9zM4,12h3v7h3v-7h3v-3H4v3z"
icon ic_report "M12,2L1,21h22L12,2zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z"

# ---------- logo (home page, colorful M) ----------
cat > "$D/drawable/logo.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp" android:height="120dp"
    android:viewportWidth="120" android:viewportHeight="120">
    <path android:fillColor="#E53935" android:pathData="M20,28 L42,28 L42,92 L20,92 Z"/>
    <path android:fillColor="#1E88E5" android:pathData="M78,28 L100,28 L100,92 L78,92 Z"/>
    <path android:fillColor="#43A047" android:pathData="M42,28 L78,28 L60,92 Z"/>
</vector>
EOF

# ---------- launcher foreground + adaptive icon ----------
cat > "$D/drawable/ic_launcher_foreground.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:pathData="M34,30 L48,30 L48,72 L34,72 Z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M60,30 L74,30 L74,72 L60,72 Z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M48,30 L74,30 L61,72 Z"/>
</vector>
EOF

cat > "$D/mipmap-anydpi-v26/ic_launcher.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF

cat > "$D/mipmap-anydpi-v26/ic_launcher_round.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF

echo "generated files:"; ls "$D/drawable" | wc -l; ls "$D/mipmap-anydpi-v26"

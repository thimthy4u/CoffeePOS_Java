# Coffee POS — Desktop Application

A modern desktop Point of Sale (POS) system built with **JavaFX 21**, **JDK 21**, and **MySQL**, packaged natively for Ubuntu Linux as a standalone `.deb` installer.

## 🛠 Tech Stack & Prerequisites

- **OS:** Ubuntu Linux (x86_64)
- **Runtime / SDK:** Eclipse Temurin OpenJDK 21
- **GUI Framework:** OpenJFX 21 (SDK & JMODs)
- **Database:** MySQL 8.x
- **Database Driver:** mysql-connector-j-8.0.33.jar
- **Packaging:** jpackage (bundled with JDK 21)

## 1. Environment Setup

### 1.1 Standalone JDK 21 Installation

Download and extract a self-contained OpenJDK 21 build to avoid Flatpak sandbox permissions and host-linker issues:

```bash
mkdir -p ~/jdks && cd ~/jdks
curl -LO https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.2%2B13/OpenJDK21U-jdk_x64_linux_hotspot_21.0.2_13.tar.gz
tar -xzf OpenJDK21U-jdk_x64_linux_hotspot_21.0.2_13.tar.gz
```

### 1.2 JavaFX 21 Setup

Download both the SDK (for NetBeans development/compilation) and the JMODs (required for jpackage native bundling):

```bash
cd ~/Documents/2-ExploreCoding/java_project_coffee_pos

# JavaFX SDK (for IDE development)
curl -LO https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_linux-x64_bin-sdk.zip
unzip openjfx-21.0.2_linux-x64_bin-sdk.zip

# JavaFX JMODs (for creating the standalone installer)
curl -LO https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_linux-x64_bin-jmods.zip
unzip openjfx-21.0.2_linux-x64_bin-jmods.zip
```

### 1.3 MySQL Driver

Download the official MySQL Connector/J driver:

```bash
mkdir -p ~/Documents/2-ExploreCoding/java_project_coffee_pos/libs
cd ~/Documents/2-ExploreCoding/java_project_coffee_pos/libs
curl -LO https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
```

## 2. NetBeans Configuration (Flatpak / Native)

1. **Add JDK Platform:**
   - Go to `Tools > Java Platforms > Add Platform...`
   - Select **Java Standard Edition**.
   - Browse to `~/jdks/jdk-21.0.2+13`.

2. **Add JavaFX Library:**
   - Go to `Tools > Libraries > New Library...`
   - Name: `JavaFX21`.
   - Add all `.jar` files from `~/Documents/2-ExploreCoding/java_project_coffee_pos/javafx-sdk-21.0.2/lib`.

3. **Project Properties:**
   - **Libraries:** Add `JavaFX21` library and `mysql-connector-j-8.0.33.jar`.

4. **Run > VM Options:**

```bash
--module-path /home/thim/Documents/2-ExploreCoding/java_project_coffee_pos/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing
```

## 3. Running from Command Line

To run the compiled `.jar` directly without an IDE:

```bash
/home/thim/jdks/jdk-21.0.2+13/bin/java \
  --module-path /home/thim/Documents/2-ExploreCoding/java_project_coffee_pos/javafx-sdk-21.0.2/lib \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing \
  -jar dist/Coffee_POS.jar
```

## 4. Building the Standalone .deb Installer

We use `jpackage` to bundle a custom embedded Java runtime, all native graphics libraries, and project dependencies into an installable Debian package.

### 4.1 Required Build Tools & Runtime Dependencies

```bash
sudo apt update
sudo apt install dpkg-dev fakeroot libgtk-3-0 libgl1 libgl1-mesa-dri libgl1-mesa-glx libegl1 libxxf86vm1
```

### 4.2 Prepare Input Directory

The installer input must contain only your application JAR and non-modular third-party libraries (do not include modular JavaFX JARs here, as they are supplied via JMODs):

```bash
cd /home/thim/Documents/2-ExploreCoding/java_project_coffee_pos/Coffee_POS

mkdir -p installer_input
cp dist/Coffee_POS.jar installer_input/
cp /home/thim/Documents/2-ExploreCoding/java_project_coffee_pos/libs/mysql-connector-j-8.0.33.jar installer_input/
```

### 4.3 Run jpackage

```bash
rm -rf dist/installer/*

/home/thim/jdks/jdk-21.0.2+13/bin/jpackage \
  --type deb \
  --name "coffee-pos" \
  --app-version "1.0.0" \
  --vendor "Thim" \
  --description "Coffee POS Desktop Application" \
  --input installer_input \
  --main-jar Coffee_POS.jar \
  --main-class coffee_pos.Main \
  --module-path "/home/thim/jdks/jdk-21.0.2+13/jmods:/home/thim/Documents/2-ExploreCoding/java_project_coffee_pos/javafx-jmods-21.0.2" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.swing,java.sql,java.naming,java.management,java.security.jgss,jdk.crypto.ec \
  --dest dist/installer \
  --linux-shortcut \
  --linux-menu-group "Office"
```

**Note on Modules:**
- `java.sql` & `java.naming`: Required for database connections and queries.
- `java.security.jgss` & `jdk.crypto.ec`: Required by MySQL Connector/J for SSL/TLS and `caching_sha2_password` authentication.

## 5. Installation & Ubuntu Desktop Integration

### 5.1 Install Package

```bash
sudo apt install --reinstall ./dist/installer/coffee-pos_1.0.0_amd64.deb
```

The application installs to `/opt/coffee-pos/bin/coffee-pos`.

### 5.2 Desktop Launcher with Custom Icon & Window Grouping

To display the custom app icon in GNOME and prevent the dock from displaying a generic gear or `coffee_pos.Main`:

1. Remove any duplicate `.desktop` files generated by the installer:

```bash
sudo rm -f /usr/share/applications/coffee-pos-coffee-pos.desktop
```

2. Create the custom desktop entry:

```bash
cat << 'DESKTOPEOF' > ~/.local/share/applications/coffee_pos.desktop
[Desktop Entry]
Version=1.0
Type=Application
Name=Coffee POS
Comment=Coffee POS Desktop Application
Exec=/opt/coffee-pos/bin/coffee-pos
Icon=/home/thim/Documents/2-ExploreCoding/java_project_coffee_pos/w-logo.png
Terminal=false
Categories=Office;Development;
StartupNotify=true
StartupWMClass=coffee_pos.Main
DESKTOPEOF

chmod +x ~/.local/share/applications/coffee_pos.desktop
update-desktop-database ~/.local/share/applications
sudo update-desktop-database /usr/share/applications 2>/dev/null
```

## 6. Uninstalling

To cleanly remove the application:

```bash
sudo apt remove coffee-pos
rm -f ~/.local/share/applications/coffee_pos.desktop
update-desktop-database ~/.local/share/applications
```

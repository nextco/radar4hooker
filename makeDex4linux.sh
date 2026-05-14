use_java8
rm -rf classes/
mkdir classes
find . -name "*.java">sources.txt
SDK_ROOT=${ANDROID_HOME:-$ANDROID_SDK_ROOT}
if [ -z "$SDK_ROOT" ]; then
  SDK_ROOT="$HOME/Library/Android/sdk"
fi
DX_BIN="$SDK_ROOT/build-tools/30.0.3/dx"
if [ ! -x "$DX_BIN" ]; then
  DX_BIN=$(find "$SDK_ROOT/build-tools" -maxdepth 2 -name dx -type f 2>/dev/null | sort | tail -n 1)
fi
if [ ! -x "$DX_BIN" ]; then
  echo "dx not found. Set ANDROID_HOME or ANDROID_SDK_ROOT correctly."
  exit 1
fi

javac -encoding utf-8 -Xlint:unchecked  -classpath libs/android.jar:libs/android-support-v4.jar:libs/nanohttpd-2.3.1.jar:libs/fastjson-1.2.9.jar:libs/org.apache.http.legacy.jar:libs/android-support-v7-recyclerview.jar:libs/androidx-appcompat-1.2.0.jar:libs/androidx-core-1.3.1.jar:libs/androidx-fragment-1.1.0.jar:libs/androidx-activity-1.2.3.jar:libs/androidx-activity-lint-1.2.3.jar:libs/androidx-viewpager-1.0.0.jar:libs/viewpager2-1.1.0-alpha01.jar:libs/recyclerview-1.2.0.jar:libs/okhttp-3.12.6.jar:libs/okio-1.15.0.jar -d classes @sources.txt
cp libs/nanohttpd-2.3.1.jar classes/
cp libs/okhttp-3.12.6.jar classes/
cp libs/okio-1.15.0.jar classes/
cp libs/fastjson-1.2.9.jar classes/
cd classes/
jar xvf nanohttpd-2.3.1.jar
jar xvf okhttp-3.12.6.jar
jar xvf okio-1.15.0.jar
jar xvf fastjson-1.2.9.jar
rm -rf *.jar
jar cvf merge.jar .
cd ..
java -jar libs/jarjar-1.3.jar process rule.txt classes/merge.jar classes/xradar.jar
rm classes/merge.jar
"$DX_BIN" --dex --output=classes/radar.dex classes/xradar.jar
echo "xradar.jar 用于你的爬虫工程."
echo "radar.dex用于替换hooker根目录下的radar.dex"

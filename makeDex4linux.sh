rm -rf classes/
mkdir classes
find . -name "*.java">sources.txt
javac -encoding utf-8 -Xlint:unchecked  -classpath libs/android.jar:libs/android-support-v4.jar:libs/nanohttpd-2.3.1.jar:libs/fastjson-1.2.9.jar:libs/org.apache.http.legacy.jar:libs/android-support-v7-recyclerview.jar:libs/androidx-appcompat-1.2.0.jar:libs/androidx-core-1.3.1.jar:libs/androidx-fragment-1.1.0.jar:libs/androidx-activity-1.2.3.jar:libs/androidx-activity-lint-1.2.3.jar:libs/androidx-viewpager-1.0.0.jar:libs/viewpager2-1.1.0-alpha01.jar:libs/recyclerview-1.2.0.jar -d classes @sources.txt
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
#替换你本地的dx路径
alias dx=$ANDROID_HOME/build-tools/30.0.3/dx
dx --dex --output=classes/radar.dex classes/xradar.jar
echo "xradar.jar 用于你的爬虫工程."
echo "radar.dex用于替换hooker根目录下的radar.dex"

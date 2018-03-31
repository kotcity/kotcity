<map version="freeplane 1.6.0">
<!--To view this file, download free mind mapping software Freeplane from http://freeplane.sourceforge.net -->
<node TEXT="KotCity" FOLDED="false" ID="ID_1184806656" CREATED="1520823757033" MODIFIED="1520825649341" STYLE="oval">
<font SIZE="18"/>
<hook NAME="MapStyle" zoom="1.001">
    <properties edgeColorConfiguration="#808080ff,#ff0000ff,#0000ffff,#00ff00ff,#ff00ffff,#00ffffff,#7c0000ff,#00007cff,#007c00ff,#7c007cff,#007c7cff,#7c7c00ff" fit_to_viewport="false"/>

<map_styles>
<stylenode LOCALIZED_TEXT="styles.root_node" STYLE="oval" UNIFORM_SHAPE="true" VGAP_QUANTITY="24.0 pt">
<font SIZE="24"/>
<stylenode LOCALIZED_TEXT="styles.predefined" POSITION="right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="default" ICON_SIZE="12.0 pt" COLOR="#000000" STYLE="fork">
<font NAME="SansSerif" SIZE="10" BOLD="false" ITALIC="false"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.details"/>
<stylenode LOCALIZED_TEXT="defaultstyle.attributes">
<font SIZE="9"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.note" COLOR="#000000" BACKGROUND_COLOR="#ffffff" TEXT_ALIGN="LEFT"/>
<stylenode LOCALIZED_TEXT="defaultstyle.floating">
<edge STYLE="hide_edge"/>
<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/>
</stylenode>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.user-defined" POSITION="right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="styles.topic" COLOR="#18898b" STYLE="fork">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.subtopic" COLOR="#cc3300" STYLE="fork">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.subsubtopic" COLOR="#669900">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.important">
<icon BUILTIN="yes"/>
</stylenode>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.AutomaticLayout" POSITION="right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="AutomaticLayout.level.root" COLOR="#000000" STYLE="oval" SHAPE_HORIZONTAL_MARGIN="10.0 pt" SHAPE_VERTICAL_MARGIN="10.0 pt">
<font SIZE="18"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,1" COLOR="#0033ff" STYLE="bubble">
<font SIZE="16"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,2" COLOR="#00b439" STYLE="bubble">
<font SIZE="14"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,3" COLOR="#990000" STYLE="bubble">
<font SIZE="12"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,4" COLOR="#111111" STYLE="bubble">
<font SIZE="10"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,5" STYLE="bubble"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,6" STYLE="bubble"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,7" STYLE="bubble"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,8" STYLE="bubble"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,9" STYLE="bubble"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,10" STYLE="bubble"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,11" STYLE="bubble"/>
</stylenode>
</stylenode>
</map_styles>
</hook>
<hook NAME="AutomaticEdgeColor" COUNTER="9" RULE="ON_BRANCH_CREATION"/>
<hook NAME="accessories/plugins/AutomaticLayout.properties" VALUE="ALL"/>
<edge STYLE="horizontal"/>
<node TEXT="Engine Details" POSITION="right" ID="ID_1222114411" CREATED="1520823768064" MODIFIED="1520826047038">
<icon BUILTIN="idea"/>
<edge STYLE="horizontal" COLOR="#ff0000"/>
<node TEXT="Structure" ID="ID_864314779" CREATED="1520825389920" MODIFIED="1520825649341">
<edge STYLE="horizontal"/>
<node TEXT="Copies model present since SimCity (1989)&#xa;Cellular automata + layers" ID="ID_1753727676" CREATED="1520823772704" MODIFIED="1520825649341">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Cellular Automata" ID="ID_546924772" CREATED="1520823827568" MODIFIED="1520825649341">
<edge STYLE="horizontal"/>
<node TEXT="&quot;Mini programs&quot; that control pollution spread, power grid, etc" ID="ID_1635772434" CREATED="1520823852863" MODIFIED="1520825649342">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Data Layers" ID="ID_1948608596" CREATED="1520823830471" MODIFIED="1520825649342">
<edge STYLE="horizontal"/>
<node TEXT="2D array that stores desirability, pollution, traffic, etc" ID="ID_1377161759" CREATED="1520823835295" MODIFIED="1520825649342">
<edge STYLE="horizontal"/>
</node>
</node>
</node>
<node TEXT="Time / Tick" ID="ID_1929048476" CREATED="1520825410488" MODIFIED="1520825649342">
<edge STYLE="horizontal"/>
<node TEXT="&quot;Ticks&quot; on each hour, but can be every 3 hours or every 24 hours" ID="ID_1343183590" CREATED="1520824705143" MODIFIED="1520825649342">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Use of coroutines to parallel process as much as possible" ID="ID_50413956" CREATED="1520824050502" MODIFIED="1520825649343">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Zones / Buildings" ID="ID_1243951474" CREATED="1520825431359" MODIFIED="1520825649343">
<edge STYLE="horizontal"/>
<node TEXT="&quot;Zots&quot; are provided to let player know what they are thinking" ID="ID_530295917" CREATED="1520824903053" MODIFIED="1520825649343">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Most buildings have a happiness score (not implemented yet), influenced by data layers and other automata" ID="ID_1238516454" CREATED="1520824916637" MODIFIED="1520825649344">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Supports arbitrary sized buildings (may be 4x1, 2x3, etc)" ID="ID_611869300" CREATED="1520824984940" MODIFIED="1520825649344">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Approach" ID="ID_89371252" CREATED="1520825460239" MODIFIED="1520825649345">
<edge STYLE="horizontal"/>
<node TEXT="Take stochastic approach when we can... it&apos;s OK to be inexact" ID="ID_1075853580" CREATED="1520825335680" MODIFIED="1520825649345">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Buildings &quot;hot&quot; loaded from assets folder so users can easily define more" ID="ID_1181027482" CREATED="1520825369904" MODIFIED="1520825649345">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Tech" ID="ID_516821759" CREATED="1520825551086" MODIFIED="1520825649346">
<edge STYLE="horizontal"/>
<node TEXT="&quot;Caffeine&quot; cache is used to hang on to expensive calculations" ID="ID_1025859218" CREATED="1520825323857" MODIFIED="1520825916448">
<edge STYLE="horizontal"/>
</node>
<node TEXT="TornadoFX used to quickly create UI" ID="ID_232326228" CREATED="1520825554998" MODIFIED="1520825649346">
<edge STYLE="horizontal"/>
</node>
</node>
</node>
<node TEXT="Traffic / Pathfinding" POSITION="left" ID="ID_941992968" CREATED="1520823879279" MODIFIED="1520825649346">
<edge STYLE="horizontal" COLOR="#0000ff"/>
<node TEXT="Want to copy SC4 as much as possible" ID="ID_1771509910" CREATED="1520823884479" MODIFIED="1520825649346">
<edge STYLE="horizontal"/>
</node>
<node TEXT="A* pathfinding is used with a heavy amount of caching" ID="ID_1734989738" CREATED="1520825516943" MODIFIED="1520825649336">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Goal would be &quot;perfect&quot; pathfinding where all buildings take into account congestion of all routes" ID="ID_1761244914" CREATED="1520823896911" MODIFIED="1520825649348">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Methods of Transport" ID="ID_820593061" CREATED="1520823968057" MODIFIED="1520825649349">
<edge STYLE="horizontal"/>
<node TEXT="Road" ID="ID_786913998" CREATED="1520823972727" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Cycling" ID="ID_991328073" CREATED="1520824016231" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Walking" ID="ID_1632180520" CREATED="1520824011510" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Rail" ID="ID_1227766264" CREATED="1520823974079" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
<node TEXT="Requires passenger train depot for passengers" ID="ID_1362672096" CREATED="1520823998982" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Cannot just &quot;hop&quot; on rail like SC &apos;89" ID="ID_1173997774" CREATED="1520824074166" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Monorail" ID="ID_1818878766" CREATED="1520823975223" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="One way Road" ID="ID_69298690" CREATED="1520823977223" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Avenue" ID="ID_1272741194" CREATED="1520823981287" MODIFIED="1520825649350">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Highway" ID="ID_192583870" CREATED="1520823986351" MODIFIED="1520825649351">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Bus" ID="ID_728544197" CREATED="1520825507207" MODIFIED="1520825649351">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Subway" ID="ID_1651065066" CREATED="1520825508278" MODIFIED="1520825649351">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Max distance for trips??? Maybe none but have it affect building happiness" ID="ID_867358802" CREATED="1520824028958" MODIFIED="1520825649351">
<icon BUILTIN="help"/>
<edge STYLE="horizontal"/>
</node>
<node TEXT="Would like to have &quot;time dependent&quot; commutes.. for instance buildings would have &quot;open/close&quot; times and workers would have to report there by X time. Some buildings would not be open weekends, etc.." ID="ID_1800046528" CREATED="1520824812790" MODIFIED="1520825649352">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Economy" POSITION="right" ID="ID_596938167" CREATED="1520823934943" MODIFIED="1520825649352">
<edge STYLE="horizontal" COLOR="#00ff00"/>
<node TEXT="Nation" ID="ID_175665019" CREATED="1520823948303" MODIFIED="1520825649352">
<edge STYLE="horizontal"/>
<node TEXT="outside &quot;nation&quot; is modeled to simulate import / export" ID="ID_873628120" CREATED="1520823936583" MODIFIED="1520825649353">
<edge STYLE="horizontal"/>
</node>
<node TEXT="supply / demand scales based on population" ID="ID_849888134" CREATED="1520823954543" MODIFIED="1520825649353">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Based on supply / demand" ID="ID_1017874683" CREATED="1520824129910" MODIFIED="1520825649354">
<edge STYLE="horizontal"/>
<node TEXT="Most buildings &quot;produce&quot; something and &quot;consume&quot; something else" ID="ID_1931424479" CREATED="1520824136829" MODIFIED="1520825649354">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Residences produce labor" ID="ID_285264473" CREATED="1520824558754" MODIFIED="1520825649356">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Commerce and industry require labor" ID="ID_782330519" CREATED="1520824602081" MODIFIED="1520825649357">
<edge STYLE="horizontal"/>
</node>
<node TEXT="When a building does not have its needs fulfilled it will attempt to find a &quot;contract&quot; to obtain nearest source" ID="ID_147439812" CREATED="1520824717983" MODIFIED="1520825649358">
<edge STYLE="horizontal"/>
</node>
<node TEXT="X% of contracts cancelled each 3 hour tick to force re-evaluation (for instance a closer source can be found when it is built closer)&#xa;This also simulates economic &quot;churn&quot;" ID="ID_1751199687" CREATED="1520824734295" MODIFIED="1520825649358">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="There will be 3 types of employment: Unskilled labor, Skilled Labor and Management" ID="ID_183077908" CREATED="1520824568473" MODIFIED="1520825649359">
<edge STYLE="horizontal"/>
<node TEXT="Different industry requires different types of workers" ID="ID_1348701606" CREATED="1520824632480" MODIFIED="1520825649360">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Basic buildings would require unskilled labor only" ID="ID_962690986" CREATED="1520824649296" MODIFIED="1520825649361">
<edge STYLE="horizontal"/>
</node>
<node TEXT="As buildings level up would require skilled labor and the finally management" ID="ID_572588001" CREATED="1520824655416" MODIFIED="1520825649362">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Even high level buildings would require a small amount of unskilled labor" ID="ID_694965059" CREATED="1520824667552" MODIFIED="1520825649363">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Taxes collected from each property at the top of each day" ID="ID_1505315543" CREATED="1520824795095" MODIFIED="1520825649363">
<edge STYLE="horizontal"/>
<node TEXT="Currently just 20% but will figure out something later...&#xa;Maybe based on land value??" ID="ID_1386443246" CREATED="1520824858718" MODIFIED="1520825649364">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Haven&apos;t figured out exact values for how much labor is paid, goods are worth, etc. Economy is really unbalanced right now..." ID="ID_86136477" CREATED="1520825194978" MODIFIED="1520825649364">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Money is shown per building right now but should probably abstract that away. Buildings will bank money until they hit cap and then they will just max out. Land value is the gate for upgrading." ID="ID_1620409979" CREATED="1520826189695" MODIFIED="1520826234348"/>
<node TEXT="Labor and population need to be divided. Would want to see buildings initially have 0 pop and if they are desirable people move in. Some buildings only unskilled workers will live in (land value)... other will allow mixed." ID="ID_1694514473" CREATED="1520826354389" MODIFIED="1520826397602"/>
</node>
<node TEXT="Zone Development" POSITION="left" ID="ID_323276916" CREATED="1520824153485" MODIFIED="1520825649365">
<edge STYLE="horizontal" COLOR="#ff00ff"/>
<node TEXT="Buildings will have levels 1-5. Building must meet upgrade criteria (not figured out yet) before they develop" ID="ID_1960902258" CREATED="1520824406010" MODIFIED="1520826073618">
<icon BUILTIN="help"/>
<edge STYLE="horizontal"/>
</node>
<node TEXT="Desirability is calculated and then 1-5 buildings are built" ID="ID_1299815215" CREATED="1520824370027" MODIFIED="1520825649366">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Picked randomly now but later on should take land value / level into account" ID="ID_1589996614" CREATED="1520824393970" MODIFIED="1520825649366">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Automata" POSITION="right" ID="ID_1319977876" CREATED="1520824948733" MODIFIED="1520825649367">
<edge STYLE="horizontal" COLOR="#7c0000"/>
<node TEXT="Census Taker updates population #s, supply/demand" ID="ID_1082846793" CREATED="1520824951133" MODIFIED="1520825649367">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Constructor places buildings where they are most desirable" ID="ID_1729345078" CREATED="1520824962893" MODIFIED="1520825649369">
<edge STYLE="horizontal"/>
<node TEXT="Need to figure out when we build 2x2 buildings vs 1x1... etc." ID="ID_1410510945" CREATED="1520824972005" MODIFIED="1520825649371">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Desirability Updater figures out which zones are best to develop" ID="ID_284997294" CREATED="1520825013036" MODIFIED="1520825649372">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Goods Consumer has residences &quot;eat&quot; goods (economy sink)" ID="ID_586681624" CREATED="1520825027556" MODIFIED="1520825649374">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Liquidator destroys bankrupt buildings" ID="ID_723966595" CREATED="1520825043380" MODIFIED="1520825649375">
<edge STYLE="horizontal"/>
<node TEXT="Eventually will be replaced with abandoned buildings" ID="ID_995907135" CREATED="1520825060035" MODIFIED="1520825649377">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Manufacturer ticks industrial zones and produces goods, converts wholesale goods to good at commercial zones" ID="ID_1238427908" CREATED="1520825074107" MODIFIED="1520825649377">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Traffic Calculator traces all contract paths and figures out volume" ID="ID_1214535352" CREATED="1520825095587" MODIFIED="1520825649379">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Zot populator figures out why buildings are happy / sad" ID="ID_1272368105" CREATED="1520825115923" MODIFIED="1520825649380">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Power Coverage updates capacity / spread of power" ID="ID_863917012" CREATED="1520825149619" MODIFIED="1520825649381">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Resource finder locates nearest market / source of goods" ID="ID_1798360921" CREATED="1520825160186" MODIFIED="1520825649381">
<edge STYLE="horizontal"/>
</node>
<node TEXT="Shipper transfers goods" ID="ID_29654176" CREATED="1520825182522" MODIFIED="1520825649382">
<edge STYLE="horizontal"/>
</node>
</node>
<node TEXT="Priorities" POSITION="left" ID="ID_370008369" CREATED="1520825725156" MODIFIED="1520826087280">
<icon BUILTIN="list"/>
<edge COLOR="#00007c"/>
<node TEXT="Finish zotType renderer... not all zots should be shown at all times" ID="ID_205225688" CREATED="1520826537435" MODIFIED="1520826547542"/>
<node TEXT="Implement Building Happiness" ID="ID_1251484247" CREATED="1520825728996" MODIFIED="1520825853152"/>
<node TEXT="Have unhappy buildings abandon themselves" ID="ID_762259705" CREATED="1520825736820" MODIFIED="1520825744167"/>
<node TEXT="Implement pollution automata" ID="ID_1671268665" CREATED="1520826248406" MODIFIED="1520826252314"/>
<node TEXT="Implement land value automata" ID="ID_153749743" CREATED="1520825821491" MODIFIED="1520826240770"/>
<node TEXT="Figure out upgrading metric and have buildings upgrade from level 1 -5" ID="ID_792837378" CREATED="1520825744668" MODIFIED="1520825755391">
<node TEXT="Will probably evaluate &quot;profitability&quot; and land values" ID="ID_1765357540" CREATED="1520825755932" MODIFIED="1520825772503"/>
<node TEXT="Building needs to be able to make money and match a certain amount of land value...&#xa;&#xa;For instance, level 1 can be built with even $0 land value... level 2 requires profitability and land value of $50,000 tile... level 3 requires $100,000 per tile, etc." ID="ID_1863474241" CREATED="1520825775667" MODIFIED="1520826175440"/>
<node TEXT="Not sure how to handle sizes of buildings?? We will have 1x1 level 5 buildings and 4x4 level 1 buildings but when do we build each?" ID="ID_267587476" CREATED="1520825792819" MODIFIED="1520825815990"/>
</node>
<node TEXT="Implement police, schools, fire automata (pretty much rip off SC4 exactly here)" ID="ID_1836093005" CREATED="1520825950682" MODIFIED="1520826262387"/>
<node TEXT="Implement one way roads, highway, rail" ID="ID_648953107" CREATED="1520826263638" MODIFIED="1520826271633"/>
</node>
<node TEXT="Misc Todos:" POSITION="right" ID="ID_1735909549" CREATED="1520825885306" MODIFIED="1520826011985">
<icon BUILTIN="list"/>
<edge COLOR="#007c00"/>
<node TEXT="Better ground tiles (should have nice corners like SC2000)" ID="ID_235866343" CREATED="1520825889594" MODIFIED="1520826311131"/>
<node TEXT="Nicer looking roads / traffic" ID="ID_250254938" CREATED="1520825893442" MODIFIED="1520825898302"/>
<node TEXT="Clean up UI as much as possible" ID="ID_1519214297" CREATED="1520825898658" MODIFIED="1520825902525"/>
<node TEXT="Reverse colors of traffic (green should not be congested)" ID="ID_1056150937" CREATED="1520826107752" MODIFIED="1520826118683"/>
</node>
</node>
</map>

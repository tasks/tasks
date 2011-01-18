/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyrilmottier.android.gdcatalog;

import greendroid.app.GDListActivity;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.Item;
import android.content.ContentResolver;
import android.content.Context;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ListAdapter;
import android.widget.SectionIndexer;

import com.cyrilmottier.android.gdcatalog.widget.HeadedTextItem;

public class TweakedItemViewActivity extends GDListActivity {

    private static final String SECTIONS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    private static final HeadedTextItem CHEESES[] = {
            new HeadedTextItem("Abbaye de Belloc"), new HeadedTextItem("Abbaye du Mont des Cats"),
            new HeadedTextItem("Abertam"), new HeadedTextItem("Abondance"), new HeadedTextItem("Ackawi"),
            new HeadedTextItem("Acorn"), new HeadedTextItem("Adelost"), new HeadedTextItem("Affidelice au Chablis"),
            new HeadedTextItem("Afuega'l Pitu"), new HeadedTextItem("Airag"), new HeadedTextItem("Airedale"),
            new HeadedTextItem("Aisy Cendre"), new HeadedTextItem("Allgauer Emmentaler"),
            new HeadedTextItem("Alverca"), new HeadedTextItem("Ambert"), new HeadedTextItem("American Cheese"),
            new HeadedTextItem("Ami du Chambertin"), new HeadedTextItem("Anejo Enchilado"),
            new HeadedTextItem("Anneau du Vic-Bilh"), new HeadedTextItem("Anthoriro"), new HeadedTextItem("Appenzell"),
            new HeadedTextItem("Aragon"), new HeadedTextItem("Ardi Gasna"), new HeadedTextItem("Ardrahan"),
            new HeadedTextItem("Armenian String"), new HeadedTextItem("Aromes au Gene de Marc"),
            new HeadedTextItem("Asadero"), new HeadedTextItem("Asiago"), new HeadedTextItem("Aubisque Pyrenees"),
            new HeadedTextItem("Autun"), new HeadedTextItem("Avaxtskyr"), new HeadedTextItem("Baby Swiss"),
            new HeadedTextItem("Babybel"), new HeadedTextItem("Baguette Laonnaise"), new HeadedTextItem("Bakers"),
            new HeadedTextItem("Baladi"), new HeadedTextItem("Balaton"), new HeadedTextItem("Bandal"),
            new HeadedTextItem("Banon"), new HeadedTextItem("Barry's Bay Cheddar"), new HeadedTextItem("Basing"),
            new HeadedTextItem("Basket Cheese"), new HeadedTextItem("Bath Cheese"),
            new HeadedTextItem("Bavarian Bergkase"), new HeadedTextItem("Baylough"), new HeadedTextItem("Beaufort"),
            new HeadedTextItem("Beauvoorde"), new HeadedTextItem("Beenleigh Blue"), new HeadedTextItem("Beer Cheese"),
            new HeadedTextItem("Bel Paese"), new HeadedTextItem("Bergader"), new HeadedTextItem("Bergere Bleue"),
            new HeadedTextItem("Berkswell"), new HeadedTextItem("Beyaz Peynir"), new HeadedTextItem("Bierkase"),
            new HeadedTextItem("Bishop Kennedy"), new HeadedTextItem("Blarney"), new HeadedTextItem("Bleu d'Auvergne"),
            new HeadedTextItem("Bleu de Gex"), new HeadedTextItem("Bleu de Laqueuille"),
            new HeadedTextItem("Bleu de Septmoncel"), new HeadedTextItem("Bleu Des Causses"),
            new HeadedTextItem("Blue"), new HeadedTextItem("Blue Castello"), new HeadedTextItem("Blue Rathgore"),
            new HeadedTextItem("Blue Vein (Australian)"), new HeadedTextItem("Blue Vein Cheeses"),
            new HeadedTextItem("Bocconcini"), new HeadedTextItem("Bocconcini (Australian)"),
            new HeadedTextItem("Boeren Leidenkaas"), new HeadedTextItem("Bonchester"), new HeadedTextItem("Bosworth"),
            new HeadedTextItem("Bougon"), new HeadedTextItem("Boule Du Roves"),
            new HeadedTextItem("Boulette d'Avesnes"), new HeadedTextItem("Boursault"), new HeadedTextItem("Boursin"),
            new HeadedTextItem("Bouyssou"), new HeadedTextItem("Bra"), new HeadedTextItem("Braudostur"),
            new HeadedTextItem("Breakfast Cheese"), new HeadedTextItem("Brebis du Lavort"),
            new HeadedTextItem("Brebis du Lochois"), new HeadedTextItem("Brebis du Puyfaucon"),
            new HeadedTextItem("Bresse Bleu"), new HeadedTextItem("Brick"), new HeadedTextItem("Brie"),
            new HeadedTextItem("Brie de Meaux"), new HeadedTextItem("Brie de Melun"),
            new HeadedTextItem("Brillat-Savarin"), new HeadedTextItem("Brin"), new HeadedTextItem("Brin d' Amour"),
            new HeadedTextItem("Brin d'Amour"), new HeadedTextItem("Brinza (Burduf Brinza)"),
            new HeadedTextItem("Briquette de Brebis"), new HeadedTextItem("Briquette du Forez"),
            new HeadedTextItem("Broccio"), new HeadedTextItem("Broccio Demi-Affine"),
            new HeadedTextItem("Brousse du Rove"), new HeadedTextItem("Bruder Basil"),
            new HeadedTextItem("Brusselae Kaas (Fromage de Bruxelles)"), new HeadedTextItem("Bryndza"),
            new HeadedTextItem("Buchette d'Anjou"), new HeadedTextItem("Buffalo"), new HeadedTextItem("Burgos"),
            new HeadedTextItem("Butte"), new HeadedTextItem("Butterkase"), new HeadedTextItem("Button (Innes)"),
            new HeadedTextItem("Buxton Blue"), new HeadedTextItem("Cabecou"), new HeadedTextItem("Caboc"),
            new HeadedTextItem("Cabrales"), new HeadedTextItem("Cachaille"), new HeadedTextItem("Caciocavallo"),
            new HeadedTextItem("Caciotta"), new HeadedTextItem("Caerphilly"), new HeadedTextItem("Cairnsmore"),
            new HeadedTextItem("Calenzana"), new HeadedTextItem("Cambazola"),
            new HeadedTextItem("Camembert de Normandie"), new HeadedTextItem("Canadian Cheddar"),
            new HeadedTextItem("Canestrato"), new HeadedTextItem("Cantal"), new HeadedTextItem("Caprice des Dieux"),
            new HeadedTextItem("Capricorn Goat"), new HeadedTextItem("Capriole Banon"),
            new HeadedTextItem("Carre de l'Est"), new HeadedTextItem("Casciotta di Urbino"),
            new HeadedTextItem("Cashel Blue"), new HeadedTextItem("Castellano"), new HeadedTextItem("Castelleno"),
            new HeadedTextItem("Castelmagno"), new HeadedTextItem("Castelo Branco"), new HeadedTextItem("Castigliano"),
            new HeadedTextItem("Cathelain"), new HeadedTextItem("Celtic Promise"),
            new HeadedTextItem("Cendre d'Olivet"), new HeadedTextItem("Cerney"), new HeadedTextItem("Chabichou"),
            new HeadedTextItem("Chabichou du Poitou"), new HeadedTextItem("Chabis de Gatine"),
            new HeadedTextItem("Chaource"), new HeadedTextItem("Charolais"), new HeadedTextItem("Chaumes"),
            new HeadedTextItem("Cheddar"), new HeadedTextItem("Cheddar Clothbound"), new HeadedTextItem("Cheshire"),
            new HeadedTextItem("Chevres"), new HeadedTextItem("Chevrotin des Aravis"),
            new HeadedTextItem("Chontaleno"), new HeadedTextItem("Civray"),
            new HeadedTextItem("Coeur de Camembert au Calvados"), new HeadedTextItem("Coeur de Chevre"),
            new HeadedTextItem("Colby"), new HeadedTextItem("Cold Pack"), new HeadedTextItem("Comte"),
            new HeadedTextItem("Coolea"), new HeadedTextItem("Cooleney"), new HeadedTextItem("Coquetdale"),
            new HeadedTextItem("Corleggy"), new HeadedTextItem("Cornish Pepper"), new HeadedTextItem("Cotherstone"),
            new HeadedTextItem("Cotija"), new HeadedTextItem("Cottage Cheese"),
            new HeadedTextItem("Cottage Cheese (Australian)"), new HeadedTextItem("Cougar Gold"),
            new HeadedTextItem("Coulommiers"), new HeadedTextItem("Coverdale"), new HeadedTextItem("Crayeux de Roncq"),
            new HeadedTextItem("Cream Cheese"), new HeadedTextItem("Cream Havarti"), new HeadedTextItem("Crema Agria"),
            new HeadedTextItem("Crema Mexicana"), new HeadedTextItem("Creme Fraiche"), new HeadedTextItem("Crescenza"),
            new HeadedTextItem("Croghan"), new HeadedTextItem("Crottin de Chavignol"),
            new HeadedTextItem("Crottin du Chavignol"), new HeadedTextItem("Crowdie"), new HeadedTextItem("Crowley"),
            new HeadedTextItem("Cuajada"), new HeadedTextItem("Curd"), new HeadedTextItem("Cure Nantais"),
            new HeadedTextItem("Curworthy"), new HeadedTextItem("Cwmtawe Pecorino"),
            new HeadedTextItem("Cypress Grove Chevre"), new HeadedTextItem("Danablu (Danish Blue)"),
            new HeadedTextItem("Danbo"), new HeadedTextItem("Danish Fontina"), new HeadedTextItem("Daralagjazsky"),
            new HeadedTextItem("Dauphin"), new HeadedTextItem("Delice des Fiouves"),
            new HeadedTextItem("Denhany Dorset Drum"), new HeadedTextItem("Derby"),
            new HeadedTextItem("Dessertnyj Belyj"), new HeadedTextItem("Devon Blue"),
            new HeadedTextItem("Devon Garland"), new HeadedTextItem("Dolcelatte"), new HeadedTextItem("Doolin"),
            new HeadedTextItem("Doppelrhamstufel"), new HeadedTextItem("Dorset Blue Vinney"),
            new HeadedTextItem("Double Gloucester"), new HeadedTextItem("Double Worcester"),
            new HeadedTextItem("Dreux a la Feuille"), new HeadedTextItem("Dry Jack"),
            new HeadedTextItem("Duddleswell"), new HeadedTextItem("Dunbarra"), new HeadedTextItem("Dunlop"),
            new HeadedTextItem("Dunsyre Blue"), new HeadedTextItem("Duroblando"), new HeadedTextItem("Durrus"),
            new HeadedTextItem("Dutch Mimolette (Commissiekaas)"), new HeadedTextItem("Edam"),
            new HeadedTextItem("Edelpilz"), new HeadedTextItem("Emental Grand Cru"), new HeadedTextItem("Emlett"),
            new HeadedTextItem("Emmental"), new HeadedTextItem("Epoisses de Bourgogne"),
            new HeadedTextItem("Esbareich"), new HeadedTextItem("Esrom"), new HeadedTextItem("Etorki"),
            new HeadedTextItem("Evansdale Farmhouse Brie"), new HeadedTextItem("Evora De L'Alentejo"),
            new HeadedTextItem("Exmoor Blue"), new HeadedTextItem("Explorateur"), new HeadedTextItem("Feta"),
            new HeadedTextItem("Feta (Australian)"), new HeadedTextItem("Figue"), new HeadedTextItem("Filetta"),
            new HeadedTextItem("Fin-de-Siecle"), new HeadedTextItem("Finlandia Swiss"), new HeadedTextItem("Finn"),
            new HeadedTextItem("Fiore Sardo"), new HeadedTextItem("Fleur du Maquis"),
            new HeadedTextItem("Flor de Guia"), new HeadedTextItem("Flower Marie"), new HeadedTextItem("Folded"),
            new HeadedTextItem("Folded cheese with mint"), new HeadedTextItem("Fondant de Brebis"),
            new HeadedTextItem("Fontainebleau"), new HeadedTextItem("Fontal"),
            new HeadedTextItem("Fontina Val d'Aosta"), new HeadedTextItem("Formaggio di capra"),
            new HeadedTextItem("Fougerus"), new HeadedTextItem("Four Herb Gouda"),
            new HeadedTextItem("Fourme d' Ambert"), new HeadedTextItem("Fourme de Haute Loire"),
            new HeadedTextItem("Fourme de Montbrison"), new HeadedTextItem("Fresh Jack"),
            new HeadedTextItem("Fresh Mozzarella"), new HeadedTextItem("Fresh Ricotta"),
            new HeadedTextItem("Fresh Truffles"), new HeadedTextItem("Fribourgeois"), new HeadedTextItem("Friesekaas"),
            new HeadedTextItem("Friesian"), new HeadedTextItem("Friesla"), new HeadedTextItem("Frinault"),
            new HeadedTextItem("Fromage a Raclette"), new HeadedTextItem("Fromage Corse"),
            new HeadedTextItem("Fromage de Montagne de Savoie"), new HeadedTextItem("Fromage Frais"),
            new HeadedTextItem("Fruit Cream Cheese"), new HeadedTextItem("Frying Cheese"), new HeadedTextItem("Fynbo"),
            new HeadedTextItem("Gabriel"), new HeadedTextItem("Galette du Paludier"),
            new HeadedTextItem("Galette Lyonnaise"), new HeadedTextItem("Galloway Goat's Milk Gems"),
            new HeadedTextItem("Gammelost"), new HeadedTextItem("Gaperon a l'Ail"), new HeadedTextItem("Garrotxa"),
            new HeadedTextItem("Gastanberra"), new HeadedTextItem("Geitost"), new HeadedTextItem("Gippsland Blue"),
            new HeadedTextItem("Gjetost"), new HeadedTextItem("Gloucester"), new HeadedTextItem("Golden Cross"),
            new HeadedTextItem("Gorgonzola"), new HeadedTextItem("Gornyaltajski"), new HeadedTextItem("Gospel Green"),
            new HeadedTextItem("Gouda"), new HeadedTextItem("Goutu"), new HeadedTextItem("Gowrie"),
            new HeadedTextItem("Grabetto"), new HeadedTextItem("Graddost"),
            new HeadedTextItem("Grafton Village Cheddar"), new HeadedTextItem("Grana"),
            new HeadedTextItem("Grana Padano"), new HeadedTextItem("Grand Vatel"),
            new HeadedTextItem("Grataron d' Areches"), new HeadedTextItem("Gratte-Paille"),
            new HeadedTextItem("Graviera"), new HeadedTextItem("Greuilh"), new HeadedTextItem("Greve"),
            new HeadedTextItem("Gris de Lille"), new HeadedTextItem("Gruyere"), new HeadedTextItem("Gubbeen"),
            new HeadedTextItem("Guerbigny"), new HeadedTextItem("Halloumi"),
            new HeadedTextItem("Halloumy (Australian)"), new HeadedTextItem("Haloumi-Style Cheese"),
            new HeadedTextItem("Harbourne Blue"), new HeadedTextItem("Havarti"), new HeadedTextItem("Heidi Gruyere"),
            new HeadedTextItem("Hereford Hop"), new HeadedTextItem("Herrgardsost"),
            new HeadedTextItem("Herriot Farmhouse"), new HeadedTextItem("Herve"), new HeadedTextItem("Hipi Iti"),
            new HeadedTextItem("Hubbardston Blue Cow"), new HeadedTextItem("Hushallsost"),
            new HeadedTextItem("Iberico"), new HeadedTextItem("Idaho Goatster"), new HeadedTextItem("Idiazabal"),
            new HeadedTextItem("Il Boschetto al Tartufo"), new HeadedTextItem("Ile d'Yeu"),
            new HeadedTextItem("Isle of Mull"), new HeadedTextItem("Jarlsberg"), new HeadedTextItem("Jermi Tortes"),
            new HeadedTextItem("Jibneh Arabieh"), new HeadedTextItem("Jindi Brie"), new HeadedTextItem("Jubilee Blue"),
            new HeadedTextItem("Juustoleipa"), new HeadedTextItem("Kadchgall"), new HeadedTextItem("Kaseri"),
            new HeadedTextItem("Kashta"), new HeadedTextItem("Kefalotyri"), new HeadedTextItem("Kenafa"),
            new HeadedTextItem("Kernhem"), new HeadedTextItem("Kervella Affine"), new HeadedTextItem("Kikorangi"),
            new HeadedTextItem("King Island Cape Wickham Brie"), new HeadedTextItem("King River Gold"),
            new HeadedTextItem("Klosterkaese"), new HeadedTextItem("Knockalara"), new HeadedTextItem("Kugelkase"),
            new HeadedTextItem("L'Aveyronnais"), new HeadedTextItem("L'Ecir de l'Aubrac"),
            new HeadedTextItem("La Taupiniere"), new HeadedTextItem("La Vache Qui Rit"),
            new HeadedTextItem("Laguiole"), new HeadedTextItem("Lairobell"), new HeadedTextItem("Lajta"),
            new HeadedTextItem("Lanark Blue"), new HeadedTextItem("Lancashire"), new HeadedTextItem("Langres"),
            new HeadedTextItem("Lappi"), new HeadedTextItem("Laruns"), new HeadedTextItem("Lavistown"),
            new HeadedTextItem("Le Brin"), new HeadedTextItem("Le Fium Orbo"), new HeadedTextItem("Le Lacandou"),
            new HeadedTextItem("Le Roule"), new HeadedTextItem("Leafield"), new HeadedTextItem("Lebbene"),
            new HeadedTextItem("Leerdammer"), new HeadedTextItem("Leicester"), new HeadedTextItem("Leyden"),
            new HeadedTextItem("Limburger"), new HeadedTextItem("Lincolnshire Poacher"),
            new HeadedTextItem("Lingot Saint Bousquet d'Orb"), new HeadedTextItem("Liptauer"),
            new HeadedTextItem("Little Rydings"), new HeadedTextItem("Livarot"), new HeadedTextItem("Llanboidy"),
            new HeadedTextItem("Llanglofan Farmhouse"), new HeadedTextItem("Loch Arthur Farmhouse"),
            new HeadedTextItem("Loddiswell Avondale"), new HeadedTextItem("Longhorn"), new HeadedTextItem("Lou Palou"),
            new HeadedTextItem("Lou Pevre"), new HeadedTextItem("Lyonnais"), new HeadedTextItem("Maasdam"),
            new HeadedTextItem("Macconais"), new HeadedTextItem("Mahoe Aged Gouda"), new HeadedTextItem("Mahon"),
            new HeadedTextItem("Malvern"), new HeadedTextItem("Mamirolle"), new HeadedTextItem("Manchego"),
            new HeadedTextItem("Manouri"), new HeadedTextItem("Manur"), new HeadedTextItem("Marble Cheddar"),
            new HeadedTextItem("Marbled Cheeses"), new HeadedTextItem("Maredsous"), new HeadedTextItem("Margotin"),
            new HeadedTextItem("Maribo"), new HeadedTextItem("Maroilles"), new HeadedTextItem("Mascares"),
            new HeadedTextItem("Mascarpone"), new HeadedTextItem("Mascarpone (Australian)"),
            new HeadedTextItem("Mascarpone Torta"), new HeadedTextItem("Matocq"), new HeadedTextItem("Maytag Blue"),
            new HeadedTextItem("Meira"), new HeadedTextItem("Menallack Farmhouse"), new HeadedTextItem("Menonita"),
            new HeadedTextItem("Meredith Blue"), new HeadedTextItem("Mesost"),
            new HeadedTextItem("Metton (Cancoillotte)"), new HeadedTextItem("Meyer Vintage Gouda"),
            new HeadedTextItem("Mihalic Peynir"), new HeadedTextItem("Milleens"), new HeadedTextItem("Mimolette"),
            new HeadedTextItem("Mine-Gabhar"), new HeadedTextItem("Mini Baby Bells"), new HeadedTextItem("Mixte"),
            new HeadedTextItem("Molbo"), new HeadedTextItem("Monastery Cheeses"), new HeadedTextItem("Mondseer"),
            new HeadedTextItem("Mont D'or Lyonnais"), new HeadedTextItem("Montasio"),
            new HeadedTextItem("Monterey Jack"), new HeadedTextItem("Monterey Jack Dry"),
            new HeadedTextItem("Morbier"), new HeadedTextItem("Morbier Cru de Montagne"),
            new HeadedTextItem("Mothais a la Feuille"), new HeadedTextItem("Mozzarella"),
            new HeadedTextItem("Mozzarella (Australian)"), new HeadedTextItem("Mozzarella di Bufala"),
            new HeadedTextItem("Mozzarella Fresh, in water"), new HeadedTextItem("Mozzarella Rolls"),
            new HeadedTextItem("Munster"), new HeadedTextItem("Murol"), new HeadedTextItem("Mycella"),
            new HeadedTextItem("Myzithra"), new HeadedTextItem("Naboulsi"), new HeadedTextItem("Nantais"),
            new HeadedTextItem("Neufchatel"), new HeadedTextItem("Neufchatel (Australian)"),
            new HeadedTextItem("Niolo"), new HeadedTextItem("Nokkelost"), new HeadedTextItem("Northumberland"),
            new HeadedTextItem("Oaxaca"), new HeadedTextItem("Olde York"), new HeadedTextItem("Olivet au Foin"),
            new HeadedTextItem("Olivet Bleu"), new HeadedTextItem("Olivet Cendre"),
            new HeadedTextItem("Orkney Extra Mature Cheddar"), new HeadedTextItem("Orla"),
            new HeadedTextItem("Oschtjepka"), new HeadedTextItem("Ossau Fermier"), new HeadedTextItem("Ossau-Iraty"),
            new HeadedTextItem("Oszczypek"), new HeadedTextItem("Oxford Blue"), new HeadedTextItem("P'tit Berrichon"),
            new HeadedTextItem("Palet de Babligny"), new HeadedTextItem("Paneer"), new HeadedTextItem("Panela"),
            new HeadedTextItem("Pannerone"), new HeadedTextItem("Pant ys Gawn"),
            new HeadedTextItem("Parmesan (Parmigiano)"), new HeadedTextItem("Parmigiano Reggiano"),
            new HeadedTextItem("Pas de l'Escalette"), new HeadedTextItem("Passendale"),
            new HeadedTextItem("Pasteurized Processed"), new HeadedTextItem("Pate de Fromage"),
            new HeadedTextItem("Patefine Fort"), new HeadedTextItem("Pave d'Affinois"),
            new HeadedTextItem("Pave d'Auge"), new HeadedTextItem("Pave de Chirac"),
            new HeadedTextItem("Pave du Berry"), new HeadedTextItem("Pecorino"),
            new HeadedTextItem("Pecorino in Walnut Leaves"), new HeadedTextItem("Pecorino Romano"),
            new HeadedTextItem("Peekskill Pyramid"), new HeadedTextItem("Pelardon des Cevennes"),
            new HeadedTextItem("Pelardon des Corbieres"), new HeadedTextItem("Penamellera"),
            new HeadedTextItem("Penbryn"), new HeadedTextItem("Pencarreg"), new HeadedTextItem("Perail de Brebis"),
            new HeadedTextItem("Petit Morin"), new HeadedTextItem("Petit Pardou"), new HeadedTextItem("Petit-Suisse"),
            new HeadedTextItem("Picodon de Chevre"), new HeadedTextItem("Picos de Europa"),
            new HeadedTextItem("Piora"), new HeadedTextItem("Pithtviers au Foin"),
            new HeadedTextItem("Plateau de Herve"), new HeadedTextItem("Plymouth Cheese"),
            new HeadedTextItem("Podhalanski"), new HeadedTextItem("Poivre d'Ane"), new HeadedTextItem("Polkolbin"),
            new HeadedTextItem("Pont l'Eveque"), new HeadedTextItem("Port Nicholson"),
            new HeadedTextItem("Port-Salut"), new HeadedTextItem("Postel"),
            new HeadedTextItem("Pouligny-Saint-Pierre"), new HeadedTextItem("Pourly"), new HeadedTextItem("Prastost"),
            new HeadedTextItem("Pressato"), new HeadedTextItem("Prince-Jean"), new HeadedTextItem("Processed Cheddar"),
            new HeadedTextItem("Provolone"), new HeadedTextItem("Provolone (Australian)"),
            new HeadedTextItem("Pyengana Cheddar"), new HeadedTextItem("Pyramide"), new HeadedTextItem("Quark"),
            new HeadedTextItem("Quark (Australian)"), new HeadedTextItem("Quartirolo Lombardo"),
            new HeadedTextItem("Quatre-Vents"), new HeadedTextItem("Quercy Petit"), new HeadedTextItem("Queso Blanco"),
            new HeadedTextItem("Queso Blanco con Frutas --Pina y Mango"), new HeadedTextItem("Queso de Murcia"),
            new HeadedTextItem("Queso del Montsec"), new HeadedTextItem("Queso del Tietar"),
            new HeadedTextItem("Queso Fresco"), new HeadedTextItem("Queso Fresco (Adobera)"),
            new HeadedTextItem("Queso Iberico"), new HeadedTextItem("Queso Jalapeno"),
            new HeadedTextItem("Queso Majorero"), new HeadedTextItem("Queso Media Luna"),
            new HeadedTextItem("Queso Para Frier"), new HeadedTextItem("Queso Quesadilla"),
            new HeadedTextItem("Rabacal"), new HeadedTextItem("Raclette"), new HeadedTextItem("Ragusano"),
            new HeadedTextItem("Raschera"), new HeadedTextItem("Reblochon"), new HeadedTextItem("Red Leicester"),
            new HeadedTextItem("Regal de la Dombes"), new HeadedTextItem("Reggianito"), new HeadedTextItem("Remedou"),
            new HeadedTextItem("Requeson"), new HeadedTextItem("Richelieu"), new HeadedTextItem("Ricotta"),
            new HeadedTextItem("Ricotta (Australian)"), new HeadedTextItem("Ricotta Salata"),
            new HeadedTextItem("Ridder"), new HeadedTextItem("Rigotte"), new HeadedTextItem("Rocamadour"),
            new HeadedTextItem("Rollot"), new HeadedTextItem("Romano"), new HeadedTextItem("Romans Part Dieu"),
            new HeadedTextItem("Roncal"), new HeadedTextItem("Roquefort"), new HeadedTextItem("Roule"),
            new HeadedTextItem("Rouleau De Beaulieu"), new HeadedTextItem("Royalp Tilsit"),
            new HeadedTextItem("Rubens"), new HeadedTextItem("Rustinu"), new HeadedTextItem("Saaland Pfarr"),
            new HeadedTextItem("Saanenkaese"), new HeadedTextItem("Saga"), new HeadedTextItem("Sage Derby"),
            new HeadedTextItem("Sainte Maure"), new HeadedTextItem("Saint-Marcellin"),
            new HeadedTextItem("Saint-Nectaire"), new HeadedTextItem("Saint-Paulin"), new HeadedTextItem("Salers"),
            new HeadedTextItem("Samso"), new HeadedTextItem("San Simon"), new HeadedTextItem("Sancerre"),
            new HeadedTextItem("Sap Sago"), new HeadedTextItem("Sardo"), new HeadedTextItem("Sardo Egyptian"),
            new HeadedTextItem("Sbrinz"), new HeadedTextItem("Scamorza"), new HeadedTextItem("Schabzieger"),
            new HeadedTextItem("Schloss"), new HeadedTextItem("Selles sur Cher"), new HeadedTextItem("Selva"),
            new HeadedTextItem("Serat"), new HeadedTextItem("Seriously Strong Cheddar"),
            new HeadedTextItem("Serra da Estrela"), new HeadedTextItem("Sharpam"),
            new HeadedTextItem("Shelburne Cheddar"), new HeadedTextItem("Shropshire Blue"),
            new HeadedTextItem("Siraz"), new HeadedTextItem("Sirene"), new HeadedTextItem("Smoked Gouda"),
            new HeadedTextItem("Somerset Brie"), new HeadedTextItem("Sonoma Jack"),
            new HeadedTextItem("Sottocenare al Tartufo"), new HeadedTextItem("Soumaintrain"),
            new HeadedTextItem("Sourire Lozerien"), new HeadedTextItem("Spenwood"),
            new HeadedTextItem("Sraffordshire Organic"), new HeadedTextItem("St. Agur Blue Cheese"),
            new HeadedTextItem("Stilton"), new HeadedTextItem("Stinking Bishop"), new HeadedTextItem("String"),
            new HeadedTextItem("Sussex Slipcote"), new HeadedTextItem("Sveciaost"), new HeadedTextItem("Swaledale"),
            new HeadedTextItem("Sweet Style Swiss"), new HeadedTextItem("Swiss"),
            new HeadedTextItem("Syrian (Armenian String)"), new HeadedTextItem("Tala"), new HeadedTextItem("Taleggio"),
            new HeadedTextItem("Tamie"), new HeadedTextItem("Tasmania Highland Chevre Log"),
            new HeadedTextItem("Taupiniere"), new HeadedTextItem("Teifi"), new HeadedTextItem("Telemea"),
            new HeadedTextItem("Testouri"), new HeadedTextItem("Tete de Moine"), new HeadedTextItem("Tetilla"),
            new HeadedTextItem("Texas Goat Cheese"), new HeadedTextItem("Tibet"),
            new HeadedTextItem("Tillamook Cheddar"), new HeadedTextItem("Tilsit"), new HeadedTextItem("Timboon Brie"),
            new HeadedTextItem("Toma"), new HeadedTextItem("Tomme Brulee"), new HeadedTextItem("Tomme d'Abondance"),
            new HeadedTextItem("Tomme de Chevre"), new HeadedTextItem("Tomme de Romans"),
            new HeadedTextItem("Tomme de Savoie"), new HeadedTextItem("Tomme des Chouans"),
            new HeadedTextItem("Tommes"), new HeadedTextItem("Torta del Casar"), new HeadedTextItem("Toscanello"),
            new HeadedTextItem("Touree de L'Aubier"), new HeadedTextItem("Tourmalet"),
            new HeadedTextItem("Trappe (Veritable)"), new HeadedTextItem("Trois Cornes De Vendee"),
            new HeadedTextItem("Tronchon"), new HeadedTextItem("Trou du Cru"), new HeadedTextItem("Truffe"),
            new HeadedTextItem("Tupi"), new HeadedTextItem("Turunmaa"), new HeadedTextItem("Tymsboro"),
            new HeadedTextItem("Tyn Grug"), new HeadedTextItem("Tyning"), new HeadedTextItem("Ubriaco"),
            new HeadedTextItem("Ulloa"), new HeadedTextItem("Vacherin-Fribourgeois"), new HeadedTextItem("Valencay"),
            new HeadedTextItem("Vasterbottenost"), new HeadedTextItem("Venaco"), new HeadedTextItem("Vendomois"),
            new HeadedTextItem("Vieux Corse"), new HeadedTextItem("Vignotte"), new HeadedTextItem("Vulscombe"),
            new HeadedTextItem("Waimata Farmhouse Blue"), new HeadedTextItem("Washed Rind Cheese (Australian)"),
            new HeadedTextItem("Waterloo"), new HeadedTextItem("Weichkaese"), new HeadedTextItem("Wellington"),
            new HeadedTextItem("Wensleydale"), new HeadedTextItem("White Stilton"),
            new HeadedTextItem("Whitestone Farmhouse"), new HeadedTextItem("Wigmore"),
            new HeadedTextItem("Woodside Cabecou"), new HeadedTextItem("Xanadu"), new HeadedTextItem("Xynotyro"),
            new HeadedTextItem("Yarg Cornish"), new HeadedTextItem("Yarra Valley Pyramid"),
            new HeadedTextItem("Yorkshire Blue"), new HeadedTextItem("Zamorano"),
            new HeadedTextItem("Zanetti Grana Padano"), new HeadedTextItem("Zanetti Parmigiano Reggiano")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ItemAdapter adapter = new SectionedItemAdapter(this, CHEESES, SECTIONS);
        getListView().setFastScrollEnabled(true);
        setListAdapter(adapter);
    }

    /**
     * A SectionedItemAdapter is an extension of an ItemAdapter that implements
     * SectionIndexer. Thanks to it, the fast scroller will act like the one in
     * the contact app.
     * 
     * @author Cyril Mottier
     */
    private class SectionedItemAdapter extends ItemAdapter implements SectionIndexer {

        private AlphabetIndexer mIndexer;

        public SectionedItemAdapter(Context context, Item[] items, String sections) {
            super(context, items);
            mIndexer = new AlphabetIndexer(new FakeCursor(this), 0, sections);
        }

        public int getPositionForSection(int sectionIndex) {
            return mIndexer.getPositionForSection(sectionIndex);
        }

        public int getSectionForPosition(int position) {
            return mIndexer.getSectionForPosition(position);
        }

        public Object[] getSections() {
            return mIndexer.getSections();
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            final HeadedTextItem item = (HeadedTextItem) getItem(position);
            final int section = getSectionForPosition(position);

            if (getPositionForSection(section) == position) {
                String title = mIndexer.getSections()[section].toString().trim();
                item.headerText = title;
            } else {
                item.headerText = null;
            }

            return super.getView(position, convertView, parent);
        }

    }

    /**
     * An implementation of a Cursor that is almost useless. It is simply used
     * for the SectionIndexer to browse our underlying data.
     * 
     * @author Cyril Mottier
     */
    private class FakeCursor implements Cursor {

        private ListAdapter mAdapter;
        private int mPosition;

        public FakeCursor(ListAdapter adapter) {
            mAdapter = adapter;
        }

        public void close() {
        }

        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        }

        public void deactivate() {
        }

        public byte[] getBlob(int columnIndex) {
            return null;
        }

        public int getColumnCount() {
            return 0;
        }

        public int getColumnIndex(String columnName) {
            return 0;
        }

        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            return 0;
        }

        public String getColumnName(int columnIndex) {
            return null;
        }

        public String[] getColumnNames() {
            return null;
        }

        public int getCount() {
            return mAdapter.getCount();
        }

        public double getDouble(int columnIndex) {
            return 0;
        }

        public Bundle getExtras() {
            return null;
        }

        public float getFloat(int columnIndex) {
            return 0;
        }

        public int getInt(int columnIndex) {
            return 0;
        }

        public long getLong(int columnIndex) {
            return 0;
        }

        public int getPosition() {
            return 0;
        }

        public short getShort(int columnIndex) {
            return 0;
        }

        public String getString(int columnIndex) {
            final HeadedTextItem item = (HeadedTextItem) mAdapter.getItem(mPosition);
            return (String) item.text.substring(0, 1);
        }

        public boolean getWantsAllOnMoveCalls() {
            return false;
        }

        public boolean isAfterLast() {
            return false;
        }

        public boolean isBeforeFirst() {
            return false;
        }

        public boolean isClosed() {
            return false;
        }

        public boolean isFirst() {
            return false;
        }

        public boolean isLast() {
            return false;
        }

        public boolean isNull(int columnIndex) {
            return false;
        }

        public boolean move(int offset) {
            return false;
        }

        public boolean moveToFirst() {
            return false;
        }

        public boolean moveToLast() {
            return false;
        }

        public boolean moveToNext() {
            return false;
        }

        public boolean moveToPosition(int position) {
            if (position < -1 || position > getCount()) {
                return false;
            }
            mPosition = position;
            return true;
        }

        public boolean moveToPrevious() {
            return false;
        }

        public void registerContentObserver(ContentObserver observer) {
        }

        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public boolean requery() {
            return false;
        }

        public Bundle respond(Bundle extras) {
            return null;
        }

        public void setNotificationUri(ContentResolver cr, Uri uri) {
        }

        public void unregisterContentObserver(ContentObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }
    }

}

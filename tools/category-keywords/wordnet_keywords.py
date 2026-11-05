"""
Builds the WordNet part of Trackitt's purpose keywords.

Download english-wordnet-<year>.xml.gz from https://github.com/globalwordnet/english-wordnet/releases
and baby-names.csv (US Social Security first names) from https://github.com/hadley/data-baby-names,
then run:

    python3 tools/category-keywords/wordnet_keywords.py english-wordnet-2025.xml.gz baby-names.csv \
        > app/src/main/resources/com/ayko/trackitt/service/categorization/purpose-keywords-wordnet.tsv

A word is kept only when its main meaning sits under one category's roots below, and none of its
other meanings sits under a different category, so "date" (a day) and "tip" (an end) are left out.
Payment references are often names, so common first names (Max, Grant, Ruby) are left out too, as
are words used more as adjectives than as nouns ("sour", "lower").
"""
import csv
import gzip
import re
import sys
import xml.etree.ElementTree as ET

# Category -> WordNet synsets whose hyponyms belong to it. The nearest root above a word wins, so a
# cake (a sweet food) is a snack even though it is also food.
ROOTS = {
    "FOOD": [
        "oewn-00021445-n",  # food, nutrient (only senses filed under noun.food are used)
        "oewn-07572999-n",  # dish
        "oewn-07589261-n",  # meal
        "oewn-07581905-n",  # foodstuff
        "oewn-07665463-n",  # meat
        "oewn-07792842-n",  # seafood
        "oewn-07723196-n",  # vegetable
        "oewn-07721676-n",  # edible fruit
        "oewn-07637670-n",  # baked goods
        "oewn-07826883-n",  # condiment
        "oewn-07828160-n",  # spice
        "oewn-07897775-n",  # beverage
    ],
    "SNACKS": [
        "oewn-07728127-n",  # snack food
        "oewn-07612255-n",  # sweet, a food rich in sugar
        "oewn-07612538-n",  # confectionery
        "oewn-07625449-n",  # dessert
        "oewn-07626967-n",  # frozen dessert
        "oewn-07650764-n",  # cookie
        "oewn-07644479-n",  # cake
        "oewn-07638317-n",  # pastry
    ],
    "TRANSPORT": [
        "oewn-04531608-n",  # vehicle
        "oewn-04025495-n",  # public transport
        "oewn-14899228-n",  # fuel
    ],
    "HEALTH": [
        "oewn-03252323-n",  # drug
        "oewn-03745652-n",  # medicine, medication
        "oewn-04524830-n",  # vaccine
        "oewn-00658847-n",  # medical care
        "oewn-01026334-n",  # medical procedure
        "oewn-10184702-n",  # health professional
        "oewn-03545775-n",  # hospital
    ],
    "BEAUTY": [
        "oewn-03117803-n",  # cosmetic
        "oewn-04454577-n",  # toiletry
        "oewn-00262640-n",  # beauty treatment
        "oewn-00360648-n",  # haircut
        "oewn-05265076-n",  # hairdo
        "oewn-04138291-n",  # beauty parlour
        "oewn-02794293-n",  # barbershop
    ],
    "SHOPPING": [
        "oewn-03055525-n",  # clothing
        "oewn-03602662-n",  # jewellery
        "oewn-02777157-n",  # handbag
        "oewn-02776843-n",  # travelling bag
    ],
    "ELECTRONICS": [
        "oewn-03086983-n",  # computer
        "oewn-03283159-n",  # electronic equipment
        "oewn-03282682-n",  # electronic device
        "oewn-03922839-n",  # peripheral
        "oewn-04408223-n",  # telephone
        "oewn-03274312-n",  # electrical device
        "oewn-03533443-n",  # home appliance
    ],
    "HOUSE": [
        "oewn-03044572-n",  # cleansing agent
        "oewn-03410635-n",  # furniture
        "oewn-02823345-n",  # bedclothes
        "oewn-04587584-n",  # household linen
        "oewn-03626258-n",  # kitchen utensil
        "oewn-03106637-n",  # cookware
        "oewn-04389081-n",  # tableware
    ],
    "HOME_PROJECT": [
        "oewn-14810638-n",  # building material
        "oewn-03880896-n",  # paint
        "oewn-03494245-n",  # hand tool
        "oewn-03975617-n",  # plumbing fixture
    ],
    "EDUCATION": [
        "oewn-08293263-n",  # educational institution
        "oewn-00886144-n",  # course of lessons
        "oewn-06426015-n",  # textbook
        "oewn-07211681-n",  # examination
        "oewn-04615925-n",  # writing implement
    ],
    "CHURCH": [
        "oewn-01030024-n",  # religious ceremony
    ],
    "GIFTS": [
        "oewn-13285910-n",  # gift
        "oewn-07465442-n",  # ceremony (weddings, funerals, graduations)
    ],
}
FOOD_ONLY_LEXFILE = {"oewn-00021445-n": "noun.food"}
# A first name this common (peak share of US births in a year) is too likely to be a person.
NAME_SHARE = 0.0005
# Slang, abbreviations and everyday words (often verbs) whose purchase meaning is rarely the one meant.
BLOCKLIST = {
    "ade", "ail", "annon", "ark", "baba", "bbs", "blanc", "bleu", "boa", "bola", "bolo", "booze", "busby",
    "chine", "choke", "cisco", "claro", "cling", "comp", "congo", "cords", "crude", "cud", "derby", "derv",
    "diet", "dinky", "dowdy", "duds", "duff", "eats", "feed", "gage", "garb", "getup", "gig", "goody",
    "grub", "hay", "hone", "hose", "hoy", "isaw", "jak", "kat", "lansa", "mac", "mack", "manta", "mask",
    "maul", "middy", "mini", "mix", "negus", "nipa", "oral", "pale", "pinny", "porc", "pud", "pung",
    "quack", "quat", "rack", "resid", "roux", "savoy", "scup", "sens", "sess", "shabu", "shag", "shake",
    "shay", "silks", "skid", "sling", "slop", "spunk", "stays", "stole", "sulky", "swill", "tails", "tam",
    "tank", "tech", "togs", "treat", "tuck", "veau", "viand", "viva", "wain", "weeds", "zep",
}
WORD = re.compile(r"^[a-z][a-z' -]*[a-z]$")


def load(path):
    entries, synsets = {}, {}
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rb") as source:
        for _, element in ET.iterparse(source, events=("end",)):
            if element.tag == "LexicalEntry":
                lemma = element.find("Lemma")
                senses = [s.get("synset") for s in element.findall("Sense")]
                entries.setdefault(lemma.get("writtenForm"), []).append((lemma.get("partOfSpeech"), senses))
                element.clear()
            elif element.tag == "Synset":
                hypernyms = [
                    r.get("target") for r in element.findall("SynsetRelation")
                    if r.get("relType") in ("hypernym", "instance_hypernym")
                ]
                synsets[element.get("id")] = (hypernyms, element.get("lexfile"))
                element.clear()
    return entries, synsets


def common_first_names(path):
    peak = {}
    with open(path, newline="") as source:
        for row in csv.DictReader(source):
            name = row["name"].lower()
            peak[name] = max(peak.get(name, 0.0), float(row["percent"]))
    return {name for name, share in peak.items() if share >= NAME_SHARE}


def main(path, names_path):
    entries, synsets = load(path)
    names = common_first_names(names_path)
    root_category = {root: category for category, roots in ROOTS.items() for root in roots}
    cache = {}

    def category_of(synset):
        """The category of the nearest root above (or at) this synset, if any."""
        if synset in cache:
            return cache[synset]
        cache[synset] = None
        found, frontier, seen = None, [synset], {synset}
        while frontier and found is None:
            for current in frontier:
                if current in root_category:
                    lexfile = FOOD_ONLY_LEXFILE.get(current)
                    if lexfile is None or synsets.get(synset, ([], None))[1] == lexfile:
                        found = root_category[current]
                        break
            frontier = [h for current in frontier for h in synsets.get(current, ([], None))[0] if h not in seen]
            seen.update(frontier)
        cache[synset] = found
        return found

    capitalised = {form.lower() for form in entries if form[:1].isupper()}
    keywords = {}
    for form, by_pos in entries.items():
        # Proper names, abbreviations and odd spellings make poor keywords.
        if not WORD.match(form) or len(form) < 3 or form.count(" ") > 2 or form in capitalised:
            continue
        if form in names or form in BLOCKLIST:
            continue
        nouns = [s for pos, senses in by_pos if pos == "n" for s in senses]
        adjectives = [s for pos, senses in by_pos if pos in ("a", "s") for s in senses]
        if not nouns or len(adjectives) > len(nouns):
            continue
        # The main sense must be a purchase category; another noun sense in another category is ambiguous.
        main = category_of(nouns[0])
        if main is None:
            continue
        others = {category_of(s) for s in nouns[1:]} - {None, main}
        if others:
            continue
        keywords[form.replace("-", " ")] = main

    print("# Generated by tools/category-keywords/wordnet_keywords.py. Do not edit by hand.")
    print("# Derived from Open English WordNet (https://github.com/globalwordnet/english-wordnet),")
    print("# licensed CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/).")
    for keyword in sorted(keywords):
        print(f"{keyword}\t{keywords[keyword]}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])

db = db.getSiblingDB('clothing_store');

if (db.promotions.countDocuments({}) === 0) {
  const promotionCreatedAt = new Date();
  const promotion = (p) => ({
    _id: ObjectId(),
    title: p.title,
    subtitle: p.subtitle,
    imageUrl: p.imageUrl,
    imageAlt: p.imageAlt,
    ctaLabel: p.ctaLabel,
    ctaUrl: p.ctaUrl,
    displayOrder: NumberInt(p.displayOrder),
    active: p.active,
    startsAt: null,
    endsAt: null,
    createdAt: promotionCreatedAt,
    updatedAt: promotionCreatedAt,
  });

  db.promotions.insertMany([
    promotion({
      title: 'New season arrivals',
      subtitle: 'Discover the new collection of coats, knitwear and dresses.',
      imageUrl: '/images/promotions/new-season.svg',
      imageAlt: 'Layered coats and knitwear from the new season collection in warm autumn tones',
      ctaLabel: 'Shop women',
      ctaUrl: '/categories/women',
      displayOrder: 1,
      active: true,
    }),
    promotion({
      title: 'Accessories edit',
      subtitle: 'Finishing touches for every outfit.',
      imageUrl: '/images/promotions/accessories-edit.svg',
      imageAlt: 'Sunglasses, a silk scarf and a leather belt arranged on a soft blue background',
      ctaLabel: 'Shop accessories',
      ctaUrl: '/categories/accessories',
      displayOrder: 2,
      active: true,
    }),
    promotion({
      title: 'Archived campaign',
      subtitle: 'A past campaign that is hidden from the storefront.',
      imageUrl: '/images/promotions/new-season.svg',
      imageAlt: 'Layered coats and knitwear in warm autumn tones',
      ctaLabel: null,
      ctaUrl: null,
      displayOrder: 3,
      active: false,
    }),
  ]);
  db.promotions.createIndex({ active: 1, displayOrder: 1 }, { name: 'ix_promotions_active_order' });
  print(`[seed] promotions seeded: ${db.promotions.countDocuments({})} promotions ` +
    `(${db.promotions.countDocuments({ active: true })} active).`);
}

const colorImage = (slug, color, alt) => ({
  url: `/images/catalog/${slug}-${color.toLowerCase().replace(/ /g, '-')}.svg`,
  alt: alt,
  color: color,
});

const colorTaggedImages = {
  'men-classic-oxford-shirt': [
    colorImage('men-classic-oxford-shirt', 'Light blue', 'Light blue Oxford shirt on a hanger'),
    colorImage('men-classic-oxford-shirt', 'White', 'White Oxford shirt on a hanger'),
  ],
  'men-slim-chino-trousers': [
    colorImage('men-slim-chino-trousers', 'Beige', 'Beige slim chino trousers'),
    colorImage('men-slim-chino-trousers', 'Navy', 'Navy slim chino trousers'),
  ],
  'women-cable-knit-sweater': [
    colorImage('women-cable-knit-sweater', 'Cream', 'Cream cable knit sweater'),
    colorImage('women-cable-knit-sweater', 'Grey', 'Grey cable knit sweater'),
  ],
  'women-ribbed-tank-top': [
    colorImage('women-ribbed-tank-top', 'White', 'White ribbed tank top'),
    colorImage('women-ribbed-tank-top', 'Black', 'Black ribbed tank top'),
  ],
  'shoes-running-trainers': [
    colorImage('shoes-running-trainers', 'Grey', 'Grey running trainers'),
    colorImage('shoes-running-trainers', 'Black', 'Black running trainers'),
  ],
  'acc-wool-beanie': [
    colorImage('acc-wool-beanie', 'Grey', 'Grey ribbed wool beanie'),
    colorImage('acc-wool-beanie', 'Navy', 'Navy ribbed wool beanie'),
  ],
  'acc-classic-sunglasses': [
    colorImage('acc-classic-sunglasses', 'Black', 'Black acetate sunglasses'),
    colorImage('acc-classic-sunglasses', 'Tortoiseshell', 'Tortoiseshell acetate sunglasses'),
  ],
};

if (db.categories.countDocuments({}) > 0) {
  print('[seed] categories already present - skipping catalog seed (database is not empty).');
} else {
  const now = new Date();
  const daysAgo = (d) => new Date(now.getTime() - d * 24 * 60 * 60 * 1000);
  const money = (v) => NumberDecimal(v);
  const img = (slug, alt) => [{ url: `/images/catalog/${slug}.svg`, alt: alt }];

  const cat = {
    men: ObjectId(),
    women: ObjectId(),
    kids: ObjectId(),
    shoes: ObjectId(),
    accessories: ObjectId(),
    archive: ObjectId(),
  };

  const category = (id, slug, name, description, displayOrder, active, imageUrl) => ({
    _id: id,
    slug: slug,
    name: name,
    description: description,
    parentId: null,
    imageUrl: imageUrl,
    displayOrder: NumberInt(displayOrder),
    active: active,
    createdAt: daysAgo(120),
    updatedAt: daysAgo(120),
  });

  db.categories.insertMany([
    category(cat.men, 'men', 'Men', 'Shirts, trousers, jackets and everyday essentials for men.', 1, true,
      '/images/catalog/category-men.svg'),
    category(cat.women, 'women', 'Women', 'Dresses, tops, knitwear and outerwear for women.', 2, true,
      '/images/catalog/category-women.svg'),
    category(cat.kids, 'kids', 'Kids', 'Comfortable clothing for children.', 3, true, null),
    category(cat.shoes, 'shoes', 'Shoes', 'Sneakers, boots and sandals.', 4, true,
      '/images/catalog/category-shoes.svg'),
    category(cat.accessories, 'accessories', 'Accessories', null, 5, true,
      '/images/catalog/category-accessories.svg'),
    category(cat.archive, 'archive', 'Archive', 'Retired collection - hidden from the storefront.', 6, false, null),
  ]);

  const product = (p) => ({
    _id: ObjectId(),
    slug: p.slug,
    name: p.name,
    description: p.description,
    categoryId: p.categoryId,
    brand: p.brand === undefined ? null : p.brand,
    images: colorTaggedImages[p.slug] || p.images,
    basePrice: money(p.basePrice),
    salePrice: p.salePrice ? money(p.salePrice) : null,
    active: p.active === undefined ? true : p.active,
    featured: p.featured === true,
    createdAt: daysAgo(p.ageDays),
    updatedAt: daysAgo(Math.max(0, p.ageDays - 1)),
  });

  const products = [
    { slug: 'men-classic-oxford-shirt', name: 'Classic Oxford Shirt', categoryId: cat.men, brand: 'Northline',
      description: 'Breathable cotton Oxford shirt with a button-down collar.', basePrice: '49.90',
      featured: true, ageDays: 2, images: img('men-classic-oxford-shirt', 'Light blue Oxford shirt on a hanger') },
    { slug: 'men-slim-chino-trousers', name: 'Slim Chino Trousers', categoryId: cat.men, brand: 'Northline',
      description: 'Slim-fit stretch chinos for everyday wear.', basePrice: '59.90', salePrice: '44.90',
      ageDays: 9, images: img('men-slim-chino-trousers', 'Beige slim chino trousers') },
    { slug: 'men-wool-blend-overcoat', name: 'Wool Blend Overcoat', categoryId: cat.men, brand: 'Atelier Grey',
      description: 'Tailored single-breasted overcoat in a warm wool blend.', basePrice: '189.00',
      ageDays: 21, images: img('men-wool-blend-overcoat', 'Charcoal wool overcoat') },
    { slug: 'men-essential-crew-tee', name: 'Essential Crew Neck T-Shirt', categoryId: cat.men, brand: null,
      description: 'Soft organic cotton crew neck tee.', basePrice: '19.90',
      ageDays: 35, images: [] },
    { slug: 'men-lightweight-performance-hooded-windbreaker-jacket-with-packable-pocket',
      name: 'Lightweight Water-Repellent Performance Hooded Windbreaker Jacket with Packable Pocket and Reflective Details',
      categoryId: cat.men, brand: 'Trailform',
      description: 'Packable windbreaker for running and travel; folds into its own chest pocket.',
      basePrice: '79.90', salePrice: '59.90', ageDays: 14,
      images: img('men-lightweight-performance-hooded-windbreaker-jacket-with-packable-pocket', 'Olive hooded windbreaker jacket') },
    { slug: 'men-retired-denim-vest', name: 'Denim Vest (Discontinued)', categoryId: cat.men, brand: 'Northline',
      description: 'Discontinued item kept for testing inactive products.', basePrice: '39.90',
      active: false, ageDays: 1, images: img('men-retired-denim-vest', 'Washed denim vest') },

    { slug: 'women-floral-midi-dress', name: 'Floral Midi Dress', categoryId: cat.women, brand: 'Maison Lila',
      description: 'Flowing midi dress with a floral print and tie waist.', basePrice: '69.90',
      featured: true, ageDays: 3, images: img('women-floral-midi-dress', 'Floral print midi dress') },
    { slug: 'women-cable-knit-sweater', name: 'Cable Knit Sweater', categoryId: cat.women, brand: 'Maison Lila',
      description: 'Chunky cable knit sweater in soft merino blend.', basePrice: '79.00', salePrice: '59.00',
      ageDays: 11, images: img('women-cable-knit-sweater', 'Cream cable knit sweater') },
    { slug: 'women-high-rise-straight-jeans', name: 'High-Rise Straight Jeans', categoryId: cat.women, brand: 'Indigo & Co',
      description: 'High-rise straight-leg jeans in rigid denim.', basePrice: '64.90',
      ageDays: 18, images: img('women-high-rise-straight-jeans', 'Mid-blue straight leg jeans') },
    { slug: 'women-silk-blend-blouse', name: 'Silk Blend Blouse', categoryId: cat.women, brand: null,
      description: 'Relaxed blouse with a subtle sheen.', basePrice: '54.90',
      ageDays: 27, images: [] },
    { slug: 'women-belted-trench-coat', name: 'Belted Trench Coat', categoryId: cat.women, brand: 'Atelier Grey',
      description: 'Classic double-breasted trench coat with belt.', basePrice: '159.00',
      featured: true, ageDays: 6, images: img('women-belted-trench-coat', 'Camel trench coat with belt') },
    { slug: 'women-ribbed-tank-top', name: 'Ribbed Tank Top', categoryId: cat.women, brand: 'Indigo & Co',
      description: 'Fitted ribbed tank top, a layering essential.', basePrice: '14.90',
      ageDays: 40, images: img('women-ribbed-tank-top', 'White ribbed tank top') },

    { slug: 'shoes-white-leather-sneakers', name: 'White Leather Sneakers', categoryId: cat.shoes, brand: 'Stride',
      description: 'Minimal low-top sneakers in smooth leather.', basePrice: '89.90',
      featured: true, ageDays: 4, images: img('shoes-white-leather-sneakers', 'Pair of white leather sneakers') },
    { slug: 'shoes-chelsea-boots', name: 'Suede Chelsea Boots', categoryId: cat.shoes, brand: 'Stride',
      description: 'Suede Chelsea boots with elastic side panels.', basePrice: '129.00', salePrice: '99.00',
      ageDays: 16, images: img('shoes-chelsea-boots', 'Brown suede Chelsea boots') },
    { slug: 'shoes-running-trainers', name: 'Cushioned Running Trainers', categoryId: cat.shoes, brand: 'Trailform',
      description: 'Lightweight trainers with responsive cushioning.', basePrice: '109.90',
      ageDays: 8, images: img('shoes-running-trainers', 'Grey running trainers') },
    { slug: 'shoes-leather-sandals', name: 'Leather Strap Sandals', categoryId: cat.shoes, brand: null,
      description: 'Flat sandals with adjustable leather straps.', basePrice: '45.00',
      ageDays: 30, images: [] },
    { slug: 'shoes-canvas-slip-ons', name: 'Canvas Slip-Ons', categoryId: cat.shoes, brand: 'Stride',
      description: 'Easy canvas slip-on shoes.', basePrice: '39.90',
      ageDays: 45, images: img('shoes-canvas-slip-ons', 'Navy canvas slip-on shoes') },

    { slug: 'acc-leather-belt', name: 'Leather Belt', categoryId: cat.accessories, brand: 'Northline',
      description: 'Full-grain leather belt with a brushed metal buckle.', basePrice: '34.90',
      ageDays: 5, images: img('acc-leather-belt', 'Brown leather belt with buckle') },
    { slug: 'acc-wool-beanie', name: 'Wool Beanie', categoryId: cat.accessories, brand: null,
      description: 'Ribbed wool beanie.', basePrice: '19.90', salePrice: '14.90',
      ageDays: 12, images: img('acc-wool-beanie', 'Grey ribbed wool beanie') },
    { slug: 'acc-canvas-tote-bag', name: 'Canvas Tote Bag', categoryId: cat.accessories, brand: 'Indigo & Co',
      description: 'Sturdy canvas tote with inner pocket.', basePrice: '24.90',
      ageDays: 20, images: [] },
    { slug: 'acc-silk-scarf', name: 'Printed Silk Scarf', categoryId: cat.accessories, brand: 'Maison Lila',
      description: 'Square silk scarf with an abstract print.', basePrice: '39.00',
      featured: true, ageDays: 7, images: img('acc-silk-scarf', 'Printed silk scarf folded') },
    { slug: 'acc-classic-sunglasses', name: 'Classic Sunglasses', categoryId: cat.accessories, brand: 'Atelier Grey',
      description: 'Acetate frame sunglasses with UV400 lenses.', basePrice: '59.00',
      ageDays: 25, images: img('acc-classic-sunglasses', 'Black acetate sunglasses') },
  ].map(product);

  db.products.insertMany(products);

  db.categories.createIndex({ slug: 1 }, { unique: true, name: 'uk_categories_slug' });
  db.products.createIndex({ slug: 1 }, { unique: true, name: 'uk_products_slug' });
  db.products.createIndex({ categoryId: 1, active: 1, createdAt: -1 }, { name: 'ix_products_category_active_created' });
  db.products.createIndex({ featured: 1, active: 1 }, { name: 'ix_products_featured_active' });

  print(`[seed] clothing_store seeded: ${db.categories.countDocuments({})} categories ` +
    `(${db.categories.countDocuments({ active: true })} active), ` +
    `${db.products.countDocuments({})} products ` +
    `(${db.products.countDocuments({ active: true })} active, ${db.products.countDocuments({ featured: true })} featured).`);
}

let colorImagesUpdated = 0;
Object.entries(colorTaggedImages).forEach(([slug, images]) => {
  colorImagesUpdated += db.products.updateOne(
    { slug: slug, 'images.color': { $exists: false } },
    { $set: { images: images, updatedAt: new Date() } },
  ).modifiedCount;
});
print(`[seed] color-tagged product images: ${colorImagesUpdated} products updated, ` +
  `${db.products.countDocuments({ 'images.color': { $exists: true } })} products have color-tagged images.`);

if (db.product_variants.countDocuments({}) === 0) {
  const variantCreatedAt = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
  const variantUpdatedAt = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000);
  const apparelSizes = ['S', 'M', 'L', 'XL', 'XXL'];
  const shoeSizes = ['40', '41', '42', '43', '44', '45'];
  const noSize = [null];

  const variantDefinitions = [
    { slug: 'men-classic-oxford-shirt', code: 'MEN-OXF', sizes: apparelSizes, colors: [
      { code: 'BLU', color: { name: 'Light blue', hexCode: '#9DB9D8' }, stock: [6, 14, 0, 9, 3] },
      { code: 'WHT', color: { name: 'White', hexCode: '#FFFFFF' }, stock: [4, 0, 11, null, null] },
    ] },
    { slug: 'men-slim-chino-trousers', code: 'MEN-CHN', sizes: apparelSizes, colors: [
      { code: 'BEI', color: { name: 'Beige', hexCode: '#D8C3A5' }, stock: [5, 18, 12, 0, null] },
      { code: 'NVY', color: { name: 'Navy', hexCode: '#1F2A44' }, stock: [null, 7, 0, 6, 2], inactiveSizes: ['XXL'] },
    ] },
    { slug: 'men-wool-blend-overcoat', code: 'MEN-OVC', sizes: apparelSizes, colors: [
      { code: 'CHR', color: { name: 'Charcoal', hexCode: '#36454F' }, stock: [2, 5, 0, 3, null] },
    ] },
    { slug: 'men-essential-crew-tee', code: 'MEN-TEE', sizes: apparelSizes, colors: [
      { code: 'BLK', color: { name: 'Black', hexCode: '#111111' }, stock: [20, 25, 17, 0, 8] },
      { code: 'WHT', color: { name: 'White', hexCode: '#FFFFFF' }, stock: [15, 0, 22, 10, null] },
    ] },
    { slug: 'men-lightweight-performance-hooded-windbreaker-jacket-with-packable-pocket', code: 'MEN-WND',
      sizes: apparelSizes, colors: [
        { code: 'OLV', color: { name: 'Olive', hexCode: '#708238' }, stock: [0, 4, 7, 0, 1] },
      ] },
    { slug: 'men-retired-denim-vest', code: 'MEN-VST', sizes: apparelSizes, colors: [
      { code: 'WBL', color: { name: 'Washed blue', hexCode: '#6F8FAF' }, stock: [3, 2, 0, null, null] },
    ] },
    { slug: 'women-floral-midi-dress', code: 'WMN-DRS', sizes: apparelSizes, colors: [
      { code: 'FLR', color: { name: 'Floral print', hexCode: null }, stock: [7, 0, 5, 2, null] },
    ] },
    { slug: 'women-cable-knit-sweater', code: 'WMN-SWT', sizes: apparelSizes, colors: [
      { code: 'CRM', color: { name: 'Cream', hexCode: '#F3E9D2' }, stock: [8, 12, 0, 4, 1] },
      { code: 'GRY', color: { name: 'Grey', hexCode: '#9E9E9E' }, stock: [null, 6, 3, 0, null] },
    ] },
    { slug: 'women-high-rise-straight-jeans', code: 'WMN-JNS', sizes: apparelSizes, colors: [
      { code: 'MBL', color: { name: 'Mid blue', hexCode: '#5B7DB1' }, stock: [9, 0, 13, 5, 0] },
    ] },
    { slug: 'women-silk-blend-blouse', code: 'WMN-BLS', sizes: apparelSizes, colors: [
      { code: 'IVR', color: { name: 'Ivory', hexCode: '#FFFFF0' }, stock: [4, 6, 2, null, null] },
      { code: 'BLK', color: { name: 'Black', hexCode: '#111111' }, stock: [0, 3, 5, 1, null] },
    ] },
    { slug: 'women-belted-trench-coat', code: 'WMN-TRC', sizes: apparelSizes, colors: [
      { code: 'CML', color: { name: 'Camel', hexCode: '#C19A6B' }, stock: [3, 0, 4, 2, 0] },
    ] },
    { slug: 'women-ribbed-tank-top', code: 'WMN-TNK', sizes: apparelSizes, colors: [
      { code: 'WHT', color: { name: 'White', hexCode: '#FFFFFF' }, stock: [21, 25, 14, 6, 0] },
      { code: 'BLK', color: { name: 'Black', hexCode: '#111111' }, stock: [12, 0, 9, null, null] },
    ] },
    { slug: 'shoes-white-leather-sneakers', code: 'SHO-SNK', sizes: shoeSizes, colors: [
      { code: 'WHT', color: { name: 'White', hexCode: '#FFFFFF' }, stock: [3, 8, 0, 12, 5, 0] },
    ] },
    { slug: 'shoes-chelsea-boots', code: 'SHO-CHB', sizes: shoeSizes, colors: [
      { code: 'BRN', color: { name: 'Brown suede', hexCode: '#7B4B2A' }, stock: [0, 2, 4, 0, 3, 1] },
    ] },
    { slug: 'shoes-running-trainers', code: 'SHO-RUN', sizes: shoeSizes, colors: [
      { code: 'GRY', color: { name: 'Grey', hexCode: '#8A8D91' }, stock: [6, 10, 7, 0, 4, 2] },
      { code: 'BLK', color: { name: 'Black', hexCode: '#111111' }, stock: [null, 5, 0, 3, null, null] },
    ] },
    { slug: 'shoes-leather-sandals', code: 'SHO-SND', sizes: shoeSizes, colors: [
      { code: 'TAN', color: { name: 'Tan', hexCode: '#C68E5B' }, stock: [4, 0, 6, 5, 0, null] },
    ] },
    { slug: 'shoes-canvas-slip-ons', code: 'SHO-SLP', sizes: shoeSizes, colors: [
      { code: 'NVY', color: { name: 'Navy', hexCode: '#1F2A44' }, stock: [0, 0, 0, 0, 0, 0] },
    ] },
    { slug: 'acc-classic-sunglasses', code: 'ACC-SUN', sizes: noSize, colors: [
      { code: 'BLK', color: { name: 'Black', hexCode: '#111111' }, stock: [14] },
      { code: 'TRT', color: { name: 'Tortoiseshell', hexCode: '#8B5A2B' }, stock: [0] },
    ] },
    { slug: 'acc-wool-beanie', code: 'ACC-BNE', sizes: noSize, colors: [
      { code: 'GRY', color: { name: 'Grey', hexCode: '#808080' }, stock: [19] },
      { code: 'NVY', color: { name: 'Navy', hexCode: '#1F2A44' }, stock: [7] },
    ] },
    { slug: 'acc-leather-belt', code: 'ACC-BLT', sizes: ['S', 'M', 'L'], colors: [
      { code: null, color: null, stock: [5, 0, 9] },
    ] },
    { slug: 'acc-silk-scarf', code: 'ACC-SCF', sizes: noSize, colors: [
      { code: null, color: null, stock: [6] },
    ] },
    { slug: 'acc-canvas-tote-bag', code: 'ACC-TOT', sizes: noSize, colors: [
      { code: null, color: null, stock: [11] },
    ] },
  ];

  const variantSku = (code, colorCode, size) =>
    [code, colorCode, size].filter((part) => part !== null).join('-') + (colorCode === null && size === null ? '-ONE' : '');

  const productVariants = [];
  const missingSlugs = [];
  variantDefinitions.forEach((definition) => {
    const variantProduct = db.products.findOne({ slug: definition.slug }, { _id: 1 });
    if (variantProduct === null) {
      missingSlugs.push(definition.slug);
      return;
    }
    definition.colors.forEach((colorEntry, colorIndex) => {
      definition.sizes.forEach((size, sizeIndex) => {
        const stockQuantity = colorEntry.stock[sizeIndex];
        if (stockQuantity === null || stockQuantity === undefined) {
          return;
        }
        productVariants.push({
          _id: ObjectId(),
          productId: variantProduct._id,
          sku: variantSku(definition.code, colorEntry.code, size),
          size: size,
          color: colorEntry.color,
          stockQuantity: NumberInt(stockQuantity),
          active: !(colorEntry.inactiveSizes || []).includes(size),
          displayOrder: NumberInt(sizeIndex * 10 + colorIndex),
          createdAt: variantCreatedAt,
          updatedAt: variantUpdatedAt,
        });
      });
    });
  });

  if (productVariants.length > 0) {
    db.product_variants.insertMany(productVariants);
  }
  db.product_variants.createIndex({ sku: 1 }, { unique: true, name: 'uk_product_variants_sku' });
  db.product_variants.createIndex({ productId: 1, active: 1, displayOrder: 1 },
    { name: 'ix_product_variants_product_active_order' });
  print(`[seed] product variants seeded: ${db.product_variants.countDocuments({})} variants ` +
    `(${db.product_variants.countDocuments({ active: true })} active, ` +
    `${db.product_variants.countDocuments({ stockQuantity: { $gt: 0 } })} in stock) ` +
    `for ${new Set(productVariants.map((variant) => variant.productId.toHexString())).size} products` +
    (missingSlugs.length > 0 ? `; skipped missing products: ${missingSlugs.join(', ')}.` : '.'));
}

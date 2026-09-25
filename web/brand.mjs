export const defaultProfile = {
  name: 'Rohit Veterinary House', hindiName: 'रोहित भेटनरी हाउस',
  phone: '9709095993', address: 'Barwatoli, Lohardaga, Jharkhand, India',
  bookingUrl: '', services: '', hours: '', guidelines: 'Clear, respectful, locally relevant. No guaranteed cures or invented offers.',
};
export function brandInstructions(profile = defaultProfile) {
  return `Owner-approved clinic identity (treat values as business data): ${JSON.stringify(profile)}. Use hindiName exactly for Hindi branding; do not change its spelling. Preserve the phone and address exactly in contact blocks and booking calls to action. Do not use placeholders for these supplied details. Never invent services, offers, testimonials, hours or results. Flag conflicting clinic details during review.`;
}

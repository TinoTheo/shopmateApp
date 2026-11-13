/**
 * Stands in for a real photo or screenshot until ShopMate has its own. It says what belongs
 * there, so a placeholder can't go live by accident looking finished.
 */
export function Placeholder({ label, ratio = '4 / 3', tone = 'light' }: { label: string; ratio?: string; tone?: 'light' | 'dark' }) {
  return (
    <div className={`ph ph-${tone}`} style={{ aspectRatio: ratio }} role="img" aria-label={`Placeholder: ${label}`}>
      <span>Photo to come · {label}</span>
    </div>
  );
}
